package com.powsali.loan_module.service;

import com.powsali.loan_module.entity.LoanAction;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.ActionType;
import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.repository.LoanActionRepository;
import com.powsali.loan_module.repository.LoanRepository;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanActionService {

    private final LoanActionRepository actionRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    public LoanAction recordAction(Long loanId, Long actionById, ActionType actionType, String remarks){
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(()->new RuntimeException("Loan Not Found"));
        User user = userRepository.findById(actionById)
                .orElseThrow(()->new RuntimeException("User Not Found"));

        switch (actionType){
            case FORWARDED -> {
                if(user.getRole().name().equals("DDO")){
                    loan.setStatus(LoanStatus.PENDING_HOO);
                    loan.setCurrentOwner("HOO");
                }else if(user.getRole().name().equals("EMPLOYEE")){
                    loan.setStatus(LoanStatus.PENDING_DDO);
                    loan.setCurrentOwner("DDO");
                }
            }
            case APPROVED -> {
                loan.setStatus(LoanStatus.APPROVED);
                loan.setCurrentOwner(null);
            }
            case REJECTED -> {
                loan.setStatus(LoanStatus.REJECTED);
                loan.setCurrentOwner(null);
            }
            case REVISION_REQUESTED -> {
                loan.setStatus(LoanStatus.REVISION_REQUESTED);
                loan.setCurrentOwner("EMPLOYEE");
            }
        }

        loan.setRemarks(remarks);
        loanRepository.save(loan);

        // Save action log
        LoanAction action = LoanAction.builder()
                .loanApplication(loan)
                .actionBy(user)
                .actionType(actionType)
                .remarks(remarks)
                .build();

        return actionRepository.save(action);
    }

    public List<LoanAction> getActionsForLoan(Long loanId){
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(()->new RuntimeException("Loan Not Found"));
        return actionRepository.findByLoanApplicationId(loan);
    }

}
