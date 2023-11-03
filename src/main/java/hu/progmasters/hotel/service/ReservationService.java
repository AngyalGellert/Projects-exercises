package hu.progmasters.hotel.service;

import hu.progmasters.hotel.domain.Reservation;
import hu.progmasters.hotel.dto.request.ReservationRequest;
import hu.progmasters.hotel.dto.response.ReservationDetails;
import hu.progmasters.hotel.repository.ReservationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.Valid;

@Service
@Transactional
@Slf4j
@AllArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ModelMapper modelMapper;


    public ReservationDetails recordsReservation(@Valid ReservationRequest reservation) {
        Reservation newREservation = modelMapper.map(reservation, Reservation.class);
        Reservation saved = reservationRepository.save(newREservation);
        return modelMapper.map(saved, ReservationDetails.class);
    }
}
