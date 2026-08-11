package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "tender_outcomes")
public class TenderOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id", nullable = false, unique = true)
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_vendor_id")
    private VendorSubmission winningVendor;

    @Column(name = "outcome_notes", columnDefinition = "TEXT")
    private String outcomeNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false)
    private ZonedDateTime recordedAt;

    public TenderOutcome() {}

    public TenderOutcome(UUID id, Tender tender, VendorSubmission winningVendor,
                         String outcomeNotes, User recordedBy, ZonedDateTime recordedAt) {
        this.id = id;
        this.tender = tender;
        this.winningVendor = winningVendor;
        this.outcomeNotes = outcomeNotes;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Tender tender;
        private VendorSubmission winningVendor;
        private String outcomeNotes;
        private User recordedBy;
        private ZonedDateTime recordedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tender(Tender tender) { this.tender = tender; return this; }
        public Builder winningVendor(VendorSubmission winningVendor) { this.winningVendor = winningVendor; return this; }
        public Builder outcomeNotes(String outcomeNotes) { this.outcomeNotes = outcomeNotes; return this; }
        public Builder recordedBy(User recordedBy) { this.recordedBy = recordedBy; return this; }
        public Builder recordedAt(ZonedDateTime recordedAt) { this.recordedAt = recordedAt; return this; }

        public TenderOutcome build() {
            return new TenderOutcome(id, tender, winningVendor, outcomeNotes, recordedBy, recordedAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tender getTender() { return tender; }
    public void setTender(Tender tender) { this.tender = tender; }

    public VendorSubmission getWinningVendor() { return winningVendor; }
    public void setWinningVendor(VendorSubmission winningVendor) { this.winningVendor = winningVendor; }

    public String getOutcomeNotes() { return outcomeNotes; }
    public void setOutcomeNotes(String outcomeNotes) { this.outcomeNotes = outcomeNotes; }

    public User getRecordedBy() { return recordedBy; }
    public void setRecordedBy(User recordedBy) { this.recordedBy = recordedBy; }

    public ZonedDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(ZonedDateTime recordedAt) { this.recordedAt = recordedAt; }
}
