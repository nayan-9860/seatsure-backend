package Seatsure.Backend.service;


import Seatsure.Backend.dto.CreateShowRequestDto;
import Seatsure.Backend.dto.ShowSeatResponseDto;
import Seatsure.Backend.entity.Seat;
import Seatsure.Backend.entity.Show;
import Seatsure.Backend.entity.ShowSeat;
import Seatsure.Backend.repository.SeatRepository;
import Seatsure.Backend.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowSeatService {

    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;

    public void initialiseShowSeats(
            Show show,
            CreateShowRequestDto request
    ){

        //get all seats of this shows venue
        List<Seat> seats =
                seatRepository.findByVenueId(show.getVenue().getId());

        // Create ShowSeat for every Seat
        //
        for (Seat seat : seats) {

            BigDecimal price;

            if (seat.getSeatType().name().equals("REGULAR")) {
                price = request.getRegularPrice();

            } else if (seat.getSeatType().name().equals("PREMIUM")) {
                price = request.getPremiumPrice();

            } else {
                price = request.getVipPrice();
            }

            ShowSeat showSeat = ShowSeat.builder()
                    .show(show)
                    .seat(seat)
                    .status(Seatsure.Backend.enums.ShowSeatStatus.AVAILABLE)
                    .price(price)
                    .build();

            showSeatRepository.save(showSeat);
        }
    }

    @Transactional
    public List<ShowSeatResponseDto> getShowSeats(Long showId) {

        List<ShowSeat> showSeats =
                showSeatRepository.findByShowId(showId);

        return showSeats.stream()
                .map(showSeat -> {

                    ShowSeatResponseDto response =
                            new ShowSeatResponseDto();

                    response.setShowSeatId(showSeat.getId());
                    response.setSeatId(showSeat.getSeat().getId());
                    response.setSeatNumber(
                            showSeat.getSeat().getSeatNumber()
                    );
                    response.setRowNumber(
                            showSeat.getSeat().getRowNumber()
                    );
                    response.setSeatType(
                            showSeat.getSeat().getSeatType()
                    );
                    response.setStatus(
                            showSeat.getStatus()
                    );
                    response.setPrice(
                            showSeat.getPrice()
                    );

                    return response;
                })
                .toList();
    }
}


// // 2. Create one ShowSeat for every physical Seat
//        List<ShowSeat> showSeats = seats.stream()
//                .map(seat -> {
//
//                    BigDecimal price = getPriceForSeatType(
//                            seat.getSeatType(),
//                            request
//                    );
//
//                    return ShowSeat.builder()
//                            .show(show)
//                            .seat(seat)
//                            .status(ShowSeatStatus.AVAILABLE)
//                            .price(price)
//                            .build();
//                })
//                .toList();
//
//        // 3. Save all ShowSeats
//        showSeatRepository.saveAll(showSeats);
//    }
//
//    private BigDecimal getPriceForSeatType(
//            SeatType seatType,
//            CreateShowRequestDto request) {
//
//        return switch (seatType) {
//
//            case REGULAR ->
//                    request.getRegularPrice();
//
//            case PREMIUM ->
//                    request.getPremiumPrice();
//
//            case VIP ->
//                    request.getVipPrice();
//        };

