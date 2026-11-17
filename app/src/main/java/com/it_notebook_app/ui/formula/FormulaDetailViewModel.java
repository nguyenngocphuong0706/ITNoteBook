package com.it_notebook_app.ui.formula;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.it_notebook_app.data.model.Formula;
import com.it_notebook_app.data.remote.AiService;
import com.it_notebook_app.data.repository.FormulaRepository;

/**
 * ViewModel cho màn hình chi tiết công thức.
 * Quản lý 4 tính năng AI: Giải thích, Dịch code, Phỏng vấn, Chấm điểm.
 */
public class FormulaDetailViewModel extends AndroidViewModel {

    private final FormulaRepository formulaRepository;

    public FormulaDetailViewModel(@NonNull Application application) {
        super(application);
        formulaRepository = new FormulaRepository(application);
    }

    // ─── DB operations ────────────────────────────────────────────────────────

    public LiveData<Formula> getFormula(int id)     { return formulaRepository.getById(id); }
    public void toggleFavorite(Formula formula)     { formulaRepository.setFavorite(formula.getId(), !formula.isFavorite()); }
    public void updateLastViewed(int id)            { formulaRepository.updateLastViewedAt(id, System.currentTimeMillis()); }
    public void deleteFormula(Formula formula)      { formulaRepository.delete(formula); }
    public void saveAiExplanation(Formula formula, String aiText) {
        formula.setExplanation(aiText);
        formulaRepository.update(formula);
    }

    // ─── AI LiveData ──────────────────────────────────────────────────────────

    private final MutableLiveData<AiService.AiResult> aiResult        = new MutableLiveData<>();
    private final MutableLiveData<AiService.AiResult> translateResult  = new MutableLiveData<>();
    private final MutableLiveData<AiService.AiResult> interviewResult  = new MutableLiveData<>();
    private final MutableLiveData<AiService.AiResult> gradeResult      = new MutableLiveData<>();

    public LiveData<AiService.AiResult> getAiResult()        { return aiResult; }
    public LiveData<AiService.AiResult> getTranslateResult() { return translateResult; }
    public LiveData<AiService.AiResult> getInterviewResult() { return interviewResult; }
    public LiveData<AiService.AiResult> getGradeResult()     { return gradeResult; }

    // ─── AI Actions ───────────────────────────────────────────────────────────

    /**
     * Giải thích code bằng AI.
     * Nếu đã có cache trong DB (bắt đầu bằng "## 📌"), dùng lại ngay không gọi API.
     */
    public void explainWithAi(Formula formula) {
        String cached = formula.getExplanation();
        if (cached != null && cached.startsWith("## 📌")) {
            aiResult.setValue(AiService.AiResult.success(cached));
            return;
        }
        forwardToLiveData(
                AiService.getInstance().explainCode(formula.getTitle(), formula.getContent()),
                aiResult
        );
    }

    /** Dịch code sang ngôn ngữ đích. */
    public void translateWithAi(String code, String targetLanguage) {
        forwardToLiveData(
                AiService.getInstance().translateCode(code, targetLanguage),
                translateResult
        );
    }

    /** Tạo 2 câu hỏi phỏng vấn về đoạn code. */
    public void generateInterviewQuestions(String title, String code) {
        forwardToLiveData(
                AiService.getInstance().generateInterviewQuestions(title, code),
                interviewResult
        );
    }

    /** Chấm điểm câu trả lời phỏng vấn. */
    public void gradeAnswer(String code, String question, String answer) {
        forwardToLiveData(
                AiService.getInstance().gradeAnswer(code, question, answer),
                gradeResult
        );
    }

    // ─── Private helper ───────────────────────────────────────────────────────

    /**
     * Kết nối một LiveData nguồn (từ AiService) với MutableLiveData đích (cho UI).
     * Dùng MediatorLiveData để tự động ngắt kết nối sau khi hoàn tất (SUCCESS/ERROR),
     * tránh memory leak.
     */
    @SuppressWarnings("unchecked")
    private void forwardToLiveData(LiveData<AiService.AiResult> src,
                                   MutableLiveData<AiService.AiResult> dest) {
        androidx.lifecycle.Observer<AiService.AiResult>[] observerRef = new androidx.lifecycle.Observer[1];
        MediatorLiveData<AiService.AiResult> mediator = new MediatorLiveData<>();
        mediator.addSource(src, result -> {
            dest.postValue(result);
            if (result != null && isTerminal(result.status)) {
                mediator.removeSource(src);
                if (observerRef[0] != null) {
                    mediator.removeObserver(observerRef[0]);
                }
            }
        });
        observerRef[0] = r -> { /* triggers activation */ };
        mediator.observeForever(observerRef[0]);
    }

    private boolean isTerminal(AiService.AiResult.Status status) {
        return status == AiService.AiResult.Status.SUCCESS
                || status == AiService.AiResult.Status.ERROR;
    }
}
