package hu.progmasters.hotel.service;

import hu.progmasters.hotel.domain.Reservation;
import hu.progmasters.hotel.domain.Room;
import hu.progmasters.hotel.dto.request.ImageUpload;
import hu.progmasters.hotel.dto.request.RoomForm;
import hu.progmasters.hotel.dto.request.RoomFormUpdate;
import hu.progmasters.hotel.dto.response.*;
import hu.progmasters.hotel.exception.ProfanityFoundException;
import hu.progmasters.hotel.exception.RoomAlreadyDeletedException;
import hu.progmasters.hotel.exception.RoomAlreadyExistsException;
import hu.progmasters.hotel.exception.RoomNotFoundException;
import hu.progmasters.hotel.repository.RoomRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by szfilep.
 */
@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;

    private final ModelMapper modelMapper;

    private final ImageUploadService imageUploadService;

    private final ProfanityFilterService profanityFilter;

    @Autowired
    public RoomService(RoomRepository roomRepository, ImageUploadService imageUploadService, ProfanityFilterService profanityFilter) {
        this.roomRepository = roomRepository;
        this.imageUploadService = imageUploadService;
        this.profanityFilter = profanityFilter;
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

    public RoomDetails createRoom(RoomForm roomForm) {
        if (checkIfRoomAlreadyExistsByName(roomForm.getName())) {
            throw new RoomAlreadyExistsException(roomForm.getName());
        } else if (profanityFilter.searchForProfanity(roomForm.getDescription())
                || profanityFilter.searchForProfanity(roomForm.getName())) {
            throw new ProfanityFoundException();
        } else {
            Room savedRoom = roomRepository.save(new Room(roomForm));
            List<String> newUploadedImageUrls = imageUploadService.uploadImages(roomForm.getImages());
            List<String> currentImageUrls = savedRoom.getImageUrls();
            currentImageUrls.addAll(newUploadedImageUrls);
            savedRoom.setImageUrls(currentImageUrls);
            return modelMapper.map(savedRoom, RoomDetails.class);
        }
    }

    public RoomDetails getRoomDetails(Long roomId) {
        Room room = findRoomById(roomId);
        return modelMapper.map(room, RoomDetails.class);
    }

    private void updateRoomListItemValues(RoomListItem item, Room room) {
        item.setId(room.getId());
        item.setName(room.getName());
        item.setNumberOfBeds(room.getNumberOfBeds());
        item.setPricePerNight(room.getPricePerNight());
        item.setImageUrl(room.getImageUrls());
    }

    public RoomDeletionResponse deleteRoom(Long roomId) {
        Room roomToBeDeleted = findRoomById(roomId);
        if (roomToBeDeleted.isDeleted()) {
            throw new RoomAlreadyDeletedException(roomId);
        } else {
            roomToBeDeleted.setDeleted(true);
            roomRepository.save(roomToBeDeleted);
            RoomDeletionResponse result = modelMapper.map(roomToBeDeleted, RoomDeletionResponse.class);
            result.setDeletionMessage(roomId, roomToBeDeleted.getName());
            return result;
        }
    }

    public RoomDetails updateRoomValues(@Valid RoomFormUpdate roomFormUpdate) {
        Room room = findRoomById(roomFormUpdate.getId());
        if (room.isDeleted()) {
            throw new RoomAlreadyDeletedException(room.getId());
        } else {
            if (!roomFormUpdate.getName().isBlank()) {
                room.setName(roomFormUpdate.getName());
            }
            if (roomFormUpdate.getNumberOfBeds() != null) {
                room.setNumberOfBeds(roomFormUpdate.getNumberOfBeds());
            }
            if (roomFormUpdate.getPricePerNight() != null) {
                room.setPricePerNight(roomFormUpdate.getPricePerNight());
            }
            if (!roomFormUpdate.getDescription().isBlank()) {
                room.setDescription(roomFormUpdate.getDescription());
            }
            List<String> newUploadedImageUrls = imageUploadService.uploadImages(roomFormUpdate.getImages());
            List<String> currentImageUrls = room.getImageUrls();

            currentImageUrls.addAll(newUploadedImageUrls);
            room.setImageUrls(currentImageUrls);
            roomRepository.save(room);

        }
        return new ModelMapper().map(room, RoomDetails.class);
    }


    public Room findRoomById(Long roomId) {
        Optional<Room> roomOptional = roomRepository.findById(roomId);
        return roomOptional.orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    public RoomDetailsWithReservations getRoomDetailsWithReservations(Long roomId) {
        Room room = findRoomById(roomId);
        if (!room.isDeleted()) {
            List<ReservationDetails> reservationDetailsList = new ArrayList<>();

            for (Reservation reservation : room.getReservations()) {
                if (!reservation.isDeleted()) {
                    ReservationDetails reservationDetails = modelMapper.map(reservation, ReservationDetails.class);
                    reservationDetails.setGuestEmail(reservation.getUser().getEmail());
                    reservationDetailsList.add(reservationDetails);
                }
            }

            RoomDetailsWithReservations roomDetailsWithReservations = modelMapper.map(room, RoomDetailsWithReservations.class);
            roomDetailsWithReservations.setReservationDetails(reservationDetailsList);
            return roomDetailsWithReservations;
        } else {
            throw new RoomAlreadyDeletedException(roomId);
        }

    }

    public boolean checkIfRoomAlreadyExistsByName(String roomName){
        if (roomRepository.findRoomByName(roomName) != null) {
            return true;
        }
        return false;
    }

    public RoomDetails uploadImage(Long roomId, ImageUpload imageUpload) {
        Room room = findRoomById(roomId);

        if (room.isDeleted()){
            throw new RoomAlreadyDeletedException(roomId);
        } else {
            List<String> newUploadedImageUrls = imageUploadService.uploadImages(imageUpload.getImages());
            List<String> currentImageUrls = room.getImageUrls();

            currentImageUrls.addAll(newUploadedImageUrls);
            room.setImageUrls(currentImageUrls);

            return modelMapper.map(room, RoomDetails.class);
        }
    }
}
