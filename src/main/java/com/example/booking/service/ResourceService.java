package com.example.booking.service;

import com.example.booking.dto.ResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.entity.Resource;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public List<ResourceResponse> getAll() {
        return resourceRepository.findAll().stream()
                .map(ResourceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {
        return ResourceResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Resource findEntityById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .available(request.getAvailable())
                .price(request.getPrice())
                .build();
        return ResourceResponse.fromEntity(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = findEntityById(id);
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setAvailable(request.getAvailable());
        resource.setPrice(request.getPrice());
        return ResourceResponse.fromEntity(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        resourceRepository.deleteById(id);
    }
}
