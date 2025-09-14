package com.example.cloudstorage.repository;

import com.example.cloudstorage.entity.FileEntity;
import com.example.cloudstorage.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository  extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByUserAndFilename(UserEntity user, String filename);
    List<FileEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
    boolean existsByUserAndFilename(UserEntity user, String filename);
    @Transactional
    void deleteByUserAndFilename(UserEntity user, String filename);
}
