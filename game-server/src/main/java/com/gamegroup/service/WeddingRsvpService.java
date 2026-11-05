package com.gamegroup.service;

import com.gamegroup.dto.CreateWeddingRsvpRequest;
import com.gamegroup.entity.WeddingRsvp;
import com.gamegroup.repository.WeddingRsvpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeddingRsvpService {

    private final WeddingRsvpRepository weddingRsvpRepository;

    @Transactional
    public void create(String invitationSlug, CreateWeddingRsvpRequest request) {
        WeddingRsvp rsvp = new WeddingRsvp();
        rsvp.setInvitationSlug(invitationSlug);
        rsvp.setGuestName(request.getName().trim());
        rsvp.setAttending(request.getAttending());
        rsvp.setGuestCount(Boolean.TRUE.equals(request.getAttending()) ? request.getGuestCount() : 0);
        weddingRsvpRepository.save(rsvp);
    }
}
