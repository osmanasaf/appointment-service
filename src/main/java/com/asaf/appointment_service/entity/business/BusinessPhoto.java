package com.asaf.appointment_service.entity.business;

import com.asaf.appointment_service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Public sayfa galerisinde gösterilen işletme fotoğrafı. MINI_WEBSITE özelliğine
 * sahip planlarda kullanılabilir; görsel host'lama işletme/istemci tarafından yapılır,
 * burada sadece URL saklanır (logoUrl ile aynı yaklaşım).
 */
@Entity
@Table(name = "business_photos")
public class BusinessPhoto extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 200)
    private String caption;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected BusinessPhoto() {}

    private BusinessPhoto(Builder builder) {
        this.businessId = builder.businessId;
        this.url = builder.url;
        this.caption = builder.caption;
        this.sortOrder = builder.sortOrder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateCaption(String caption) {
        this.caption = caption;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getUrl() {
        return url;
    }

    public String getCaption() {
        return caption;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public static class Builder {
        private Long businessId;
        private String url;
        private String caption;
        private int sortOrder;

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder caption(String caption) {
            this.caption = caption;
            return this;
        }

        public Builder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public BusinessPhoto build() {
            if (businessId == null) {
                throw new IllegalArgumentException("İşletme ID zorunludur");
            }
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("Fotoğraf URL'si zorunludur");
            }
            return new BusinessPhoto(this);
        }
    }
}
