package hu.progmasters.hotel.service;

import hu.progmasters.hotel.domain.Reservation;
import hu.progmasters.hotel.domain.Room;
import hu.progmasters.hotel.dto.request.RoomFormUpdate;
import hu.progmasters.hotel.dto.response.ReservationDetails;
import hu.progmasters.hotel.dto.response.RoomDetails;
import hu.progmasters.hotel.dto.request.RoomForm;
import hu.progmasters.hotel.dto.response.RoomDetailsWithReservations;
import hu.progmasters.hotel.dto.response.RoomListItem;
import hu.progmasters.hotel.repository.RoomRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by szfilep.
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;

    private final ModelMapper modelMapper;

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
        this.modelMapper = new ModelMapper();
    }

    public List<RoomListItem> getRoomList() {
        List<RoomListItem> roomListItems = new ArrayList<>();
        List<Room> rooms = roomRepository.findAll();
        for (Room room : rooms) {
            RoomListItem item = new RoomListItem();
            updateRoomListItemValues(item, room);
            roomListItems.add(item);
        }
        return roomListItems;
    }

    public void createRoom(RoomForm roomForm) {
        roomRepository.save(new Room(roomForm));
    }

    public RoomDetails getRoomDetails(Long roomId) {
        RoomDetails roomDetails = new RoomDetails();

        Optional<Room> room = roomRepository.findById(roomId);
        if (room.isPresent()) {
            roomDetails.setId(room.get().getId());
            roomDetails.setName(room.get().getName());
            roomDetails.setNumberOfBeds(room.get().getNumberOfBeds());
            roomDetails.setPricePerNight(room.get().getPricePerNight());
            roomDetails.setDescription(room.get().getDescription());
            roomDetails.setImageUrl(room.get().getImageUrl());
        } else {
            throw new IllegalArgumentException("There is no Room for this id:" + roomId);
        }
        return roomDetails;
    }

    private void updateRoomListItemValues(RoomListItem item, Room room) {
        item.setId(room.getId());
        item.setName(room.getName());
        item.setNumberOfBeds(room.getNumberOfBeds());
        item.setPricePerNight(room.getPricePerNight());
        item.setImageUrl(room.getImageUrl());
    }

    public void deleteRoom(Long roomId) {
        Optional<Room> roomToBeDeleted = roomRepository.findById(roomId);
        if (roomToBeDeleted.isPresent()) {
            roomToBeDeleted.get().setDeleted(true);
            roomRepository.save(roomToBeDeleted.get());
        }
    }

    public RoomDetails updateRoomValues(@Valid RoomFormUpdate roomFormUpdate) {
        Optional<Room> room = roomRepository.findById(roomFormUpdate.getId());
        if (room.isPresent()) {
            if (!roomFormUpdate.getName().isEmpty() || !roomFormUpdate.getName().isBlank() || !roomFormUpdate.getName().equals(room.get().getName())) {
                room.get().setName(roomFormUpdate.getName());
            }
            if (roomFormUpdate.getNumberOfBeds() != room.get().getNumberOfBeds() || roomFormUpdate.getNumberOfBeds() != 0){
                room.get().setNumberOfBeds(roomFormUpdate.getNumberOfBeds());
            }
            if(roomFormUpdate.getPricePerNight() != room.get().getPricePerNight() || roomFormUpdate.getPricePerNight() != 0){
                room.get().setPricePerNight(roomFormUpdate.getPricePerNight());
            }
            if(!roomFormUpdate.getDescription().equals(room.get().getDescription())){
                room.get().setDescription(roomFormUpdate.getDescription());
            }
            if(!roomFormUpdate.getImageUrl().equals(room.get().getImageUrl())){
                room.get().setImageUrl(roomFormUpdate.getImageUrl());
            }
            roomRepository.save(room.get());

        } else {
            throw new IllegalArgumentException("There is no Room for this id:" + roomFormUpdate.getId());
        }

        return new ModelMapper().map(room.get(), RoomDetails.class);
    }



    public Room findRoomById(Long roomId) {
        Optional<Room> roomOptional = roomRepository.findById(roomId);
        return roomOptional.orElseThrow(() -> new IllegalArgumentException("There is no Room for this id:" + roomId));
    }

    public RoomDetailsWithReservations getRoomDetailsWithReservations(Long roomId) {
        Room room = findRoomById(roomId);
        List<ReservationDetails> reservationDetailsList = new ArrayList<>();

        for (Reservation reservation : room.getReservations()) {
            ReservationDetails reservationDetails = modelMapper.map(reservation, ReservationDetails.class);
            reservationDetailsList.add(reservationDetails);
        }

        RoomDetailsWithReservations roomDetailsWithReservations = modelMapper.map(room, RoomDetailsWithReservations.class);
        roomDetailsWithReservations.setReservationDetails(reservationDetailsList);
        return roomDetailsWithReservations;

    }

}