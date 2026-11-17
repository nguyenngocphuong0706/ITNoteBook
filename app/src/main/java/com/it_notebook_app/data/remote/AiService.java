package com.it_notebook_app.data.remote;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.it_notebook_app.BuildConfig;

/**
 * Service layer cho tất cả tính năng AI (Giải thích code, Dịch code, Phỏng vấn kỹ thuật).
 * Sử dụng Groq LPU Inference API với cơ chế Streaming SSE để hiển thị kết quả real-time.
 */
public class AiService {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String BASE_URL =
            "https://api.groq.com/openai/v1/chat/completions";
    private static final MediaType JSON_MEDIA =
            MediaType.get("application/json; charset=utf-8");

    /** Model mặc định trên Groq — thay thế qwen3-32b và llama-3.1-8b đã bị Groq khai tử */
    private static final String MODEL_EXPLAIN   = "openai/gpt-oss-20b";
    /** Model nhanh — tối ưu cho dịch code & phỏng vấn */
    private static final String MODEL_FAST      = "openai/gpt-oss-20b";

    // ─── Singleton ────────────────────────────────────────────────────────────
    private static volatile AiService instance;

    private final OkHttpClient client;
    private final ExecutorService executor;

    private AiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        executor = Executors.newCachedThreadPool(); // CachedPool: tránh block khi gọi nhiều AI cùng lúc
    }

    public static AiService getInstance() {
        if (instance == null) {
            synchronized (AiService.class) {
                if (instance == null) instance = new AiService();
            }
        }
        return instance;
    }

    // ─── AiResult — Kết quả trả về ────────────────────────────────────────────

    public static final class AiResult {

        public enum Status { LOADING, STREAMING, SUCCESS, ERROR }

        public final Status status;
        public final String text;   // text tích lũy (STREAMING / SUCCESS)
        public final String error;  // thông báo lỗi (ERROR)

        private AiResult(Status status, String text, String error) {
            this.status = status;
            this.text   = text;
            this.error  = error;
        }

        public static AiResult loading()                  { return new AiResult(Status.LOADING,   null, null); }
        public static AiResult streaming(String partial)  { return new AiResult(Status.STREAMING, partial, null); }
        public static AiResult success(String text)       { return new AiResult(Status.SUCCESS,   text, null); }
        public static AiResult error(String msg)          { return new AiResult(Status.ERROR,     null, msg); }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Giải thích đoạn code bằng tiếng Việt, định dạng Markdown.
     */
    public LiveData<AiResult> explainCode(String title, String code) {
        String system = "Bạn là chuyên gia lập trình. Giải thích code bằng tiếng Việt, "
                + "định dạng Markdown. Trả lời ngắn gọn, súc tích.";
        String user = "Giải thích súc tích code: [" + title + "]\n\n"
                + "```\n" + code + "\n```\n\n"
                + "Trả về Markdown ngắn gọn theo 4 mục:\n"
                + "## 📌 Tóm tắt\n"
                + "## 🔍 Logic\n"
                + "## ⚡ Độ phức tạp\n"
                + "## 💡 Lưu ý";
        return streamRequest(MODEL_EXPLAIN, system, user);
    }

    /**
     * Dịch code sang ngôn ngữ lập trình khác với comment tiếng Việt giải thích điểm khác biệt.
     */
    public LiveData<AiResult> translateCode(String code, String targetLanguage) {
        String system = "Bạn là chuyên gia lập trình. Hãy dịch code sang " + targetLanguage + ".\n"
                + "QUY TẮC BẮT BUỘC:\n"
                + "- Chỉ trả về code " + targetLanguage + ", không giải thích bên ngoài\n"
                + "- Thêm comment tiếng Việt (// hoặc #) giải thích những điểm cú pháp KHÁC so với ngôn ngữ gốc\n"
                + "- KHÔNG dùng markdown, KHÔNG dùng backtick\n"
                + "- Tất cả comment phải bằng tiếng Việt";
        String user = "Dịch sang " + targetLanguage + " (comment tiếng Việt):\n\n" + code;
        return streamRequest(MODEL_FAST, system, user);
    }

    /**
     * Tạo 2 câu hỏi phỏng vấn kỹ thuật về đoạn code.
     */
    public LiveData<AiResult> generateInterviewQuestions(String title, String code) {
        String system = "Bạn là nhà tuyển dụng kỹ thuật senior tại một công ty công nghệ lớn. "
                + "Hãy đặt đúng 2 câu hỏi phỏng vấn bằng tiếng Việt về đoạn code được cung cấp. "
                + "Câu hỏi phải thực tế, hóc búa, kiểm tra hiểu biết sâu về thuật toán/logic. "
                + "Format:\n**Câu 1:** [câu hỏi]\n\n**Câu 2:** [câu hỏi]\n"
                + "KHÔNG trả lời câu hỏi, chỉ đặt câu hỏi.";
        String user = "Đặt 2 câu hỏi phỏng vấn về đoạn code [" + title + "]:\n\n" + code;
        return streamRequest(MODEL_FAST, system, user);
    }

    /**
     * Chấm điểm câu trả lời phỏng vấn, trả về nhận xét Markdown tiếng Việt.
     */
    public LiveData<AiResult> gradeAnswer(String code, String question, String userAnswer) {
        String system = "Bạn là nhà tuyển dụng kỹ thuật đang chấm điểm câu trả lời phỏng vấn. "
                + "Phân tích câu trả lời và trả về theo format Markdown tiếng Việt:\n"
                + "## ⭐ Điểm: X/10\n"
                + "## ✅ Điểm tốt\n"
                + "## ❌ Thiếu sót\n"
                + "## 💡 Câu trả lời lý tưởng";
        String user = "Code:\n" + code
                + "\n\nCâu hỏi: " + question
                + "\n\nCâu trả lời của ứng viên: " + userAnswer;
        return streamRequest(MODEL_FAST, system, user);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Entry point duy nhất cho mọi streaming request.
     * Tạo LiveData, submit lên executor, trả về ngay cho caller (non-blocking).
     * Hỗ trợ tự động fallback sang các model dự phòng nếu model chính gặp lỗi 404.
     */
    private LiveData<AiResult> streamRequest(String model, String systemPrompt, String userPrompt) {
        MutableLiveData<AiResult> result = new MutableLiveData<>();
        result.postValue(AiResult.loading());

        executor.execute(() -> {
            try {
                AiResult finalResult = null;
                String[] candidateModels = new String[]{model, "openai/gpt-oss-20b", "llama-3.3-70b-versatile"};
                java.util.Set<String> triedModels = new java.util.HashSet<>();

                for (String candModel : candidateModels) {
                    if (candModel == null || triedModels.contains(candModel)) continue;
                    triedModels.add(candModel);

                    JSONObject body = buildBody(candModel, systemPrompt, userPrompt);
                    finalResult = executeStream(body, result);

                    // Nếu thành công hoặc lỗi không phải 404 (ví dụ 401, 429), không cần thử model khác
                    if (finalResult != null && (finalResult.status == AiResult.Status.SUCCESS
                            || finalResult.error == null || !finalResult.error.contains("404"))) {
                        break;
                    }
                }
                result.postValue(finalResult != null ? finalResult : AiResult.error("Không nhận được phản hồi từ AI"));
            } catch (Exception e) {
                result.postValue(AiResult.error("Lỗi kết nối: " + e.getMessage()));
            }
        });

        return result;
    }

    /**
     * Xây dựng JSON body cho Groq API.
     */
    private JSONObject buildBody(String model, String systemPrompt, String userPrompt)
            throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt));

        return new JSONObject()
                .put("model", model)
                .put("stream", true)
                .put("messages", messages);
    }

    /**
     * Thực hiện HTTP request và đọc SSE stream.
     * Tự động lọc <think>...</think> block khỏi output.
     *
     * @return AiResult.success() với text cuối cùng đã được lọc
     */
    private AiResult executeStream(JSONObject body, MutableLiveData<AiResult> liveData)
            throws Exception {

        if (GROQ_API_KEY == null || GROQ_API_KEY.trim().isEmpty()) {
            return AiResult.error("Chưa cấu hình API Key trong file local.properties!\nVui lòng thêm GROQ_API_KEY vào local.properties.");
        }

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + GROQ_API_KEY.trim())
                .post(RequestBody.create(body.toString(), JSON_MEDIA))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String errorDetail = "";
                try {
                    if (response.body() != null) {
                        String errBody = response.body().string();
                        JSONObject json = new JSONObject(errBody);
                        if (json.has("error")) {
                            JSONObject errObj = json.getJSONObject("error");
                            errorDetail = errObj.optString("message", "");
                        }
                    }
                } catch (Exception ignored) {}

                if (response.code() == 401) {
                    return AiResult.error("API Key không hợp lệ (401). Vui lòng kiểm tra lại GROQ_API_KEY.");
                } else if (response.code() == 404) {
                    return AiResult.error("Mô hình AI không tìm thấy (404)" + (errorDetail.isEmpty() ? "" : ": " + errorDetail));
                } else if (response.code() == 429) {
                    return AiResult.error("Quá giới hạn gọi API (429). Vui lòng chờ vài giây rồi thử lại.");
                }
                return AiResult.error("Lỗi API (" + response.code() + ")" + (errorDetail.isEmpty() ? "" : ": " + errorDetail));
            }

            StringBuilder accumulated = new StringBuilder();
            StringBuilder pending     = new StringBuilder(); // buffer cho think-filter
            boolean inThinkBlock      = false;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;

                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;

                String chunk = parseDeltaContent(data);
                if (chunk.isEmpty()) continue;

                pending.append(chunk);
                String buf = pending.toString();

                // ── Think-block filter (state machine) ──────────────────────
                while (true) {
                    if (!inThinkBlock) {
                        int thinkStart = buf.indexOf("<think>");
                        if (thinkStart >= 0) {
                            accumulated.append(buf, 0, thinkStart);
                            buf = buf.substring(thinkStart + 7);
                            inThinkBlock = true;
                        } else {
                            accumulated.append(buf);
                            buf = "";
                            break;
                        }
                    } else {
                        int thinkEnd = buf.indexOf("</think>");
                        if (thinkEnd >= 0) {
                            buf = buf.substring(thinkEnd + 8);
                            inThinkBlock = false;
                        } else {
                            buf = ""; // đang trong think block, bỏ qua
                            break;
                        }
                    }
                }
                pending = new StringBuilder(buf);
                // ────────────────────────────────────────────────────────────

                if (accumulated.length() > 0) {
                    liveData.postValue(AiResult.streaming(accumulated.toString()));
                }
            }

            String finalText = accumulated.toString().trim();
            return finalText.isEmpty()
                    ? AiResult.error("Không nhận được phản hồi từ AI")
                    : AiResult.success(finalText);
        }
    }

    /**
     * Trích xuất content text từ một SSE data chunk.
     * Format JSON: {"choices":[{"delta":{"content":"..."},...}],...}
     */
    private String parseDeltaContent(String sseData) {
        try {
            JSONObject obj     = new JSONObject(sseData);
            JSONArray  choices = obj.getJSONArray("choices");
            if (choices.length() == 0) return "";
            JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
            return delta != null ? delta.optString("content", "") : "";
        } catch (Exception e) {
            return "";
        }
    }
}
