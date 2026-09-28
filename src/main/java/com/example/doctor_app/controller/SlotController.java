package com.example.doctor_app.controller;

import com.example.doctor_app.dto.PublishSlotsRequest;
import com.example.doctor_app.dto.SlotResponse;
import com.example.doctor_app.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SlotController {
    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping("/doctors/{doctorId}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SlotResponse> publish(@PathVariable Long doctorId,
                                     @Valid @RequestBody PublishSlotsRequest request) {
        return slotService.publish(doctorId, request).stream().map(SlotResponse::from).toList();
    }

    @GetMapping("/slots")
    public List<SlotResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String specialization) {
        return slotService.search(date, specialization).stream().map(SlotResponse::from).toList();
    }
}
