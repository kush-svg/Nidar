package com.example.nidar.evidence.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.nidar.evidence.model.LocationSnapshot;
import com.example.nidar.evidence.repository.LocationSnapshotRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationSnapshotServiceTest {

    @Mock
    private LocationSnapshotRepository snapshotRepository;

    @Mock
    private HashChainService hashChainService;

    @InjectMocks
    private LocationSnapshotService locationSnapshotService;

    @Test
    void record_FirstSnapshot_UsesGenesisAsPreviousHash() {
        when(snapshotRepository.findTopBySosSessionIdOrderByCapturedAtDesc("s1"))
            .thenReturn(Optional.empty());
        when(hashChainService.computeChainHash(eq("GENESIS"), anyString()))
            .thenReturn("computed-chain-hash");

        locationSnapshotService.record("s1", "u1", 28.6139, 77.2090, 10.0f);

        ArgumentCaptor<LocationSnapshot> captor = ArgumentCaptor.forClass(LocationSnapshot.class);
        verify(snapshotRepository).save(captor.capture());

        LocationSnapshot saved = captor.getValue();
        assertEquals("s1", saved.getSosSessionId());
        assertEquals("u1", saved.getUserId());
        assertEquals(28.6139, saved.getLatitude());
        assertEquals(77.2090, saved.getLongitude());
        assertEquals(10.0f, saved.getAccuracy());
        assertEquals("GENESIS", saved.getPreviousHash());
        assertEquals("computed-chain-hash", saved.getChainHash());
        assertNotNull(saved.getSha256Hash());
    }

    @Test
    void record_SubsequentSnapshot_UsesPreviousChainHash() {
        LocationSnapshot previous = LocationSnapshot.builder()
            .chainHash("prev-chain-hash").build();

        when(snapshotRepository.findTopBySosSessionIdOrderByCapturedAtDesc("s1"))
            .thenReturn(Optional.of(previous));
        when(hashChainService.computeChainHash(eq("prev-chain-hash"), anyString()))
            .thenReturn("new-chain-hash");

        locationSnapshotService.record("s1", "u1", 28.7000, 77.3000, 5.0f);

        ArgumentCaptor<LocationSnapshot> captor = ArgumentCaptor.forClass(LocationSnapshot.class);
        verify(snapshotRepository).save(captor.capture());

        LocationSnapshot saved = captor.getValue();
        assertEquals("prev-chain-hash", saved.getPreviousHash());
        assertEquals("new-chain-hash", saved.getChainHash());
    }

    @Test
    void record_ComputesSha256FromLocationData() {
        when(snapshotRepository.findTopBySosSessionIdOrderByCapturedAtDesc("s1"))
            .thenReturn(Optional.empty());
        when(hashChainService.computeChainHash(anyString(), anyString()))
            .thenReturn("chain-hash");

        locationSnapshotService.record("s1", "u1", 28.6139, 77.2090, 10.0f);

        ArgumentCaptor<LocationSnapshot> captor = ArgumentCaptor.forClass(LocationSnapshot.class);
        verify(snapshotRepository).save(captor.capture());

        LocationSnapshot saved = captor.getValue();
        assertNotNull(saved.getSha256Hash());
        assertEquals(64, saved.getSha256Hash().length()); // SHA-256 hex is 64 chars
    }

    @Test
    void record_DifferentCoordinatesProduceDifferentHashes() {
        when(snapshotRepository.findTopBySosSessionIdOrderByCapturedAtDesc(anyString()))
            .thenReturn(Optional.empty());
        when(hashChainService.computeChainHash(anyString(), anyString()))
            .thenReturn("chain-hash");

        locationSnapshotService.record("s1", "u1", 28.6139, 77.2090, 10.0f);
        locationSnapshotService.record("s1", "u1", 28.7000, 77.3000, 10.0f);

        ArgumentCaptor<LocationSnapshot> captor = ArgumentCaptor.forClass(LocationSnapshot.class);
        verify(snapshotRepository, times(2)).save(captor.capture());

        String hash1 = captor.getAllValues().get(0).getSha256Hash();
        String hash2 = captor.getAllValues().get(1).getSha256Hash();
        assertNotEquals(hash1, hash2);
    }
}
