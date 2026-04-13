package com.asaf.appointment_service.common.service;

import com.asaf.appointment_service.common.exception.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseService<T, ID, R extends JpaRepository<T, ID>> {

    protected final R repository;
    protected final String entityName;

    protected BaseService(R repository, String entityName) {
        this.repository = repository;
        this.entityName = entityName;
    }

    @Transactional(readOnly = true)
    public T findByIdOrThrow(ID id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(entityName, String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Deprecated(since = "1.0", forRemoval = true)
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return repository.findAll();
    }

    @Deprecated(since = "1.0", forRemoval = true)
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional
    public T save(T entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteById(ID id) {
        findByIdOrThrow(id);
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
