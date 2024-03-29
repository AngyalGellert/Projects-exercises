package hu.progmasters.hotel.service;


import hu.progmasters.hotel.domain.Hotel;
import hu.progmasters.hotel.domain.Room;
import hu.progmasters.hotel.dto.request.HotelAndRoom;
import hu.progmasters.hotel.dto.request.HotelCreateRequest;
import hu.progmasters.hotel.dto.request.ImageUpload;
import hu.progmasters.hotel.dto.response.HotelDetails;
import hu.progmasters.hotel.dto.response.HotelAndRoomInfo;
import hu.progmasters.hotel.dto.response.HotelCreationResponse;
import hu.progmasters.hotel.dto.response.RoomDetails;
import hu.progmasters.hotel.dto.response.*;
import hu.progmasters.hotel.exception.*;
import hu.progmasters.hotel.repository.HotelRepository;
import hu.progmasters.hotel.repository.RoomRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final ImageUploadService imageUploadService;
    private final ModelMapper modelMapper;
    private final OpenWeatherService openWeatherService;
    private final OpenCageGeocodingService openCageGeocodingService;

    public HotelService(HotelRepository hotelRepository, RoomService roomService,
                        RoomRepository roomRepository, ImageUploadService imageUploadService,
                        OpenWeatherService openWeatherService, OpenCageGeocodingService openCageGeocodingService) {
        this.hotelRepository = hotelRepository;
        this.roomService = roomService;
        this.roomRepository = roomRepository;
        this.openWeatherService = openWeatherService;
        this.imageUploadService = imageUploadService;
        this.openCageGeocodingService = openCageGeocodingService;
        this.modelMapper = new ModelMapper();
    }

    public HotelCreationResponse createHotel(HotelCreateRequest hotelCreateRequest) {
        if (checkIfHotelAlreadyExistsByName(hotelCreateRequest.getName())) {
            throw new HotelAlreadyExistsException(hotelCreateRequest.getName());
        } else {
            Hotel savedHotel = hotelRepository.save(modelMapper.map(hotelCreateRequest, Hotel.class));

            List<String> newUploadedImageUrls = imageUploadService.uploadImages(hotelCreateRequest.getImages());
            List<String> currentImageUrls = savedHotel.getImageUrls();
            currentImageUrls.addAll(newUploadedImageUrls);
            savedHotel.setImageUrls(currentImageUrls);

            HotelGeocodingResponse geocodingData = getGeocodingDetails(savedHotel.getId());
            savedHotel.setLatitude(geocodingData.getLatitude());
            savedHotel.setLongitude(geocodingData.getLongitude());
            return modelMapper.map(savedHotel, HotelCreationResponse.class);
        }
    }

    public HotelAndRoomInfo addRoomToHotel(HotelAndRoom hotelAndRoom) {
        Hotel hotel = findHotelById(hotelAndRoom.getHotelId());
        Room room =  roomService.findRoomById(hotelAndRoom.getRoomId());
        room.setHotel(hotel);
        roomRepository.save(room);
        return new HotelAndRoomInfo(hotel, room);
    }

    public List<RoomDetails> listAllRoomsOfHotel(Long hotelId) {
        List<Room> rooms = roomRepository.findAllAvailableRoomsFromHotel(hotelId);
        if (rooms.isEmpty()) {
            throw new HotelHasNoRoomsException(hotelId);
        } else {
            List<RoomDetails> result = new ArrayList<>();
            for (Room room : rooms) {
                result.add(modelMapper.map(room, RoomDetails.class));
            }
            return result;
        }
    }

    public Hotel findHotelById(Long hotelId) {
        Optional<Hotel> hotel = hotelRepository.findById(hotelId);
        return hotel.orElseThrow(() -> new HotelNotFoundException(hotelId));
    }

    public List<HotelDetails> listHotelDetails() {
        List<Hotel> hotels = hotelRepository.findAll();
        List<HotelDetails> hotelDetailsList = new ArrayList<>();

        for (Hotel hotel : hotels) {
            HotelDetails hotelDetails = modelMapper.map(hotel, HotelDetails.class);
            hotelDetails.setNumberOfRooms(roomRepository.numberOfAvailableRooms(hotel.getId()));
            try {
                hotelDetails.setTemperature(openWeatherService.currentWeatherInfo(hotel.getCity()).getTemperature());
                hotelDetails.setWeatherDescription(openWeatherService.currentWeatherInfo(hotel.getCity()).getWeatherDescription());
            } catch (IOException e) {
                throw new OpenWeatherException();
            }
            hotelDetailsList.add(hotelDetails);
        }
        return hotelDetailsList;
    }

    public boolean checkIfHotelAlreadyExistsByName(String hotelName){
        if (hotelRepository.findHotelByName(hotelName) != null) {
            return true;
        }
        return false;
    }

    public HotelDetails uploadImage(Long hotelId, ImageUpload imageUpload) {
        Hotel hotel = findHotelById(hotelId);

        List<String> newUploadedImageUrls = imageUploadService.uploadImages(imageUpload.getImages());
        List<String> currentImageUrls = hotel.getImageUrls();

        currentImageUrls.addAll(newUploadedImageUrls);
        hotel.setImageUrls(currentImageUrls);

        return modelMapper.map(hotel, HotelDetails.class);
    }


    public HotelDetails getDetailsFromTheHotel(Long hotelId) {
        Hotel hotel = findHotelById(hotelId);
        HotelDetails hotelDetails = modelMapper.map(hotel, HotelDetails.class);
        try {
            HotelDetails weatherInfo = openWeatherService.currentWeatherInfo(hotel.getCity());
            hotelDetails.setTemperature(weatherInfo.getTemperature());
            hotelDetails.setWeatherDescription(weatherInfo.getWeatherDescription());
        } catch (IOException e) {
            throw new OpenWeatherException();
        }
        return hotelDetails;
    }

    public ForecastResponse getForecast(Long hotelId) {
        Hotel hotel = findHotelById(hotelId);
        ForecastResponse forecastResponse = new ForecastResponse();
        try {
            forecastResponse = openWeatherService.getForecast(hotel.getCity());
        } catch (IOException e) {
            throw new OpenWeatherException();
        }
        return forecastResponse;
    }

    public HotelGeocodingResponse getGeocodingDetails(Long hotelId) {
        try {
            return openCageGeocodingService.getGeocodingDetails(findHotelById(hotelId));
        } catch (IOException e) {
            throw new GeocodingException();
        }
    }

    public List <HotelGeocodingResponse> getHotelForMap() {
        List <HotelGeocodingResponse> responses = new ArrayList<>();
        List <Hotel> hotels = hotelRepository.findAll();
        for (Hotel hotel : hotels) {
            responses.add(getGeocodingDetails(hotel.getId()));
        }
        return responses;
    }
}
