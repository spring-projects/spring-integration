package com.springframework.integration.loanbroker.loanshark.domain;

import com.springframework.integration.loanbroker.loanshark.domain.LoanShark;
import java.lang.Integer;
import java.lang.Long;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;
import org.springframework.transaction.annotation.Transactional;

privileged aspect LoanShark_Roo_Entity {
    
    @PersistenceContext
    transient EntityManager LoanShark.entityManager;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long LoanShark.id;
    
    @Version
    @Column(name = "version")
    private Integer LoanShark.version;
    
    public Long LoanShark.getId() {
        return this.id;
    }
    
    public void LoanShark.setId(Long id) {
        this.id = id;
    }
    
    public Integer LoanShark.getVersion() {
        return this.version;
    }
    
    public void LoanShark.setVersion(Integer version) {
        this.version = version;
    }
    
    @Transactional
    public void LoanShark.persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
    }
    
    @Transactional
    public void LoanShark.remove() {
        if (this.entityManager == null) this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            LoanShark attached = this.entityManager.find(LoanShark.class, this.id);
            this.entityManager.remove(attached);
        }
    }
    
    @Transactional
    public void LoanShark.flush() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.flush();
    }
    
    @Transactional
    public void LoanShark.merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        LoanShark merged = this.entityManager.merge(this);
        this.entityManager.flush();
        this.id = merged.getId();
    }
    
    public static final EntityManager LoanShark.entityManager() {
        EntityManager em = new LoanShark().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }
    
    public static long LoanShark.countLoanSharks() {
        return (Long) entityManager().createQuery("select count(o) from LoanShark o").getSingleResult();
    }
    
    public static List<LoanShark> LoanShark.findAllLoanSharks() {
        return entityManager().createQuery("select o from LoanShark o").getResultList();
    }
    
    public static LoanShark LoanShark.findLoanShark(Long id) {
        if (id == null) return null;
        return entityManager().find(LoanShark.class, id);
    }
    
    public static List<LoanShark> LoanShark.findLoanSharkEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("select o from LoanShark o").setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
    
}
