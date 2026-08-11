package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "correction_logs")
public class CorrectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id", nullable = false)
    private MatchResult matchResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "system_verdict", nullable = false, length = 20)
    private String systemVerdict;

    @Column(name = "human_verdict", nullable = false, length = 20)
    private String humanVerdict;

    @Column(name = "is_override", nullable = false)
    private boolean isOverride;

    @Column(name = "is_auto_confirmed", nullable = false)
    private boolean isAutoConfirmed = false;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String context;

    @Column(name = "user_comment", columnDefinition = "TEXT")
    private String userComment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public CorrectionLog() {}

    public CorrectionLog(UUID id, MatchResult matchResult, Tender tender, User user,
                         String systemVerdict, String humanVerdict, boolean isOverride,
                         boolean isAutoConfirmed, String context, String userComment,
                         ZonedDateTime createdAt) {
        this.id = id;
        this.matchResult = matchResult;
        this.tender = tender;
        this.user = user;
        this.systemVerdict = systemVerdict;
        this.humanVerdict = humanVerdict;
        this.isOverride = isOverride;
        this.isAutoConfirmed = isAutoConfirmed;
        this.context = context;
        this.userComment = userComment;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private MatchResult matchResult;
        private Tender tender;
        private User user;
        private String systemVerdict;
        private String humanVerdict;
        private boolean isOverride;
        private boolean isAutoConfirmed = false;
        private String context;
        private String userComment;
        private ZonedDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder matchResult(MatchResult matchResult) { this.matchResult = matchResult; return this; }
        public Builder tender(Tender tender) { this.tender = tender; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder systemVerdict(String systemVerdict) { this.systemVerdict = systemVerdict; return this; }
        public Builder humanVerdict(String humanVerdict) { this.humanVerdict = humanVerdict; return this; }
        public Builder isOverride(boolean isOverride) { this.isOverride = isOverride; return this; }
        public Builder isAutoConfirmed(boolean isAutoConfirmed) { this.isAutoConfirmed = isAutoConfirmed; return this; }
        public Builder context(String context) { this.context = context; return this; }
        public Builder userComment(String userComment) { this.userComment = userComment; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CorrectionLog build() {
            return new CorrectionLog(id, matchResult, tender, user, systemVerdict, humanVerdict,
                    isOverride, isAutoConfirmed, context, userComment, createdAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MatchResult getMatchResult() { return matchResult; }
    public void setMatchResult(MatchResult matchResult) { this.matchResult = matchResult; }

    public Tender getTender() { return tender; }
    public void setTender(Tender tender) { this.tender = tender; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSystemVerdict() { return systemVerdict; }
    public void setSystemVerdict(String systemVerdict) { this.systemVerdict = systemVerdict; }

    public String getHumanVerdict() { return humanVerdict; }
    public void setHumanVerdict(String humanVerdict) { this.humanVerdict = humanVerdict; }

    public boolean isOverride() { return isOverride; }
    public void setOverride(boolean override) { isOverride = override; }

    public boolean isAutoConfirmed() { return isAutoConfirmed; }
    public void setAutoConfirmed(boolean autoConfirmed) { isAutoConfirmed = autoConfirmed; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getUserComment() { return userComment; }
    public void setUserComment(String userComment) { this.userComment = userComment; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
