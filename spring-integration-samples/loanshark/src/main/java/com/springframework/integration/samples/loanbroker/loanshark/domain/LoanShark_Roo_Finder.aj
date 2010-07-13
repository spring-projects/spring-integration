package com.springframework.integration.samples.loanbroker.loanshark.domain;

import java.lang.String;
import java.lang.SuppressWarnings;
import javax.persistence.EntityManager;
import javax.persistence.Query;

privileged aspect LoanShark_Roo_Finder {
    
    @SuppressWarnings("unchecked")
    public static Query LoanShark.findLoanSharksByName(String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = LoanShark.entityManager();
        Query q = em.createQuery("SELECT LoanShark FROM LoanShark AS loanshark WHERE loanshark.name = :name");
        q.setParameter("name", name);
        return q;
    }
    
}
