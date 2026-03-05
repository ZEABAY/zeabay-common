package com.zeabay.common.r2dbc;

import com.zeabay.common.autoconfigure.ZeabayR2dbcAuditingAutoConfiguration;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

/**
 * Base class for all R2DBC entities.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>TSID-based {@code id} (Long / BIGINT) — autopopulated via {@code BeforeSaveCallback}
 *   <li>Reactive audit fields: createdAt, updatedAt, createdBy, updatedBy
 *   <li>Soft-delete support via {@code deletedAt} / {@code deletedBy}
 * </ul>
 *
 * <p>Usage: extend this class in every domain entity. Spring Data R2DBC will automatically populate
 * audit fields when {@code @EnableR2dbcAuditing} is present (see {@link
 * ZeabayR2dbcAuditingAutoConfiguration}).
 */
@Getter
@Setter
public abstract class BaseEntity {

  @Id private Long id;

  @CreatedDate
  @Column("created_at")
  private Instant createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private Instant updatedAt;

  @Column("deleted_at")
  private Instant deletedAt;

  @CreatedBy
  @Column("created_by")
  private String createdBy;

  @LastModifiedBy
  @Column("updated_by")
  private String updatedBy;

  @Column("deleted_by")
  private String deletedBy;

  /** Returns true if this entity has been soft-deleted. */
  public boolean isDeleted() {
    return deletedAt != null;
  }
}
