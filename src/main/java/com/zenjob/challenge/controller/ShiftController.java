package com.zenjob.challenge.controller;

import com.zenjob.challenge.dto.ResponseDto;
import com.zenjob.challenge.entity.Job;
import com.zenjob.challenge.entity.Shift;
import com.zenjob.challenge.service.JobService;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping(path = "/shift")
@RequiredArgsConstructor
public class ShiftController {
    private final JobService jobService;

    @GetMapping(path = "/{jobId}")
    @ResponseBody
    public ResponseDto<GetShiftsResponse> getShifts(@PathVariable("jobId") UUID uuid) {
        List<ShiftResponse> shiftResponses = jobService.getShifts(uuid).stream()
                .map(shift -> ShiftResponse.builder()
                        .id(uuid)
                        .talentId(shift.getTalentId())
                        .jobId(shift.getJob().getId())
                        .start(shift.getCreatedAt())
                        .end(shift.getEndTime())
                        .build())
                .collect(Collectors.toList());
        return ResponseDto.<GetShiftsResponse>builder()
                .data(GetShiftsResponse.builder()
                        .shifts(shiftResponses)
                        .build())
                .build();
    }

    @PatchMapping(path = "/{id}/book")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void bookTalent(@PathVariable("id") UUID shiftId, @RequestBody @Valid ShiftController.BookTalentRequestDto dto) {
        jobService.bookTalent(shiftId, dto.talent);
    }

    //Task B
    @DeleteMapping(path = "cancelShift/{jobId}")
    public ResponseEntity<?> cancelShift(@PathVariable("jobId") UUID jobId,
                                         @RequestParam(required = true, value = "shiftId") UUID shiftId) {
        //Find Job
        Optional<Job> jobList = jobService.getJobs(jobId);
        if (jobList.isPresent()) {
            jobService.deleteShift(shiftId);
            return ResponseEntity.ok().body("Single Shift Cancelled");
        }
        return ResponseEntity.notFound().build();
    }

    //Task C
    // Considering this as ShiftController  method since it can impact shifts cross jobs .
    @DeleteMapping(path = "cancelAll/{talentId}/{replacementTalentId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)

    public void cancelAll(@PathVariable("talentId") UUID talentId,@PathVariable("replacementTalentId") UUID replacementTalentId) {
        List<Shift> shiftsPerTalent = jobService.getShiftsPerTalent(talentId);
        if (!shiftsPerTalent.isEmpty()) {
            for (Shift shift : shiftsPerTalent) {
                // Will assume that since the current shift is been deleted
                // it will create another shift for same job & same period but for another Talent.
                jobService.replaceShift(replacementTalentId, shift.getId());
            }

        }
    }


    @NoArgsConstructor
    @Data
    private static class BookTalentRequestDto {
        UUID talent;
    }

    @Builder
    @Data
    private static class GetShiftsResponse {
        List<ShiftResponse> shifts;
    }

    @Builder
    @Data
    private static class ShiftResponse {
        UUID id;
        UUID talentId;
        UUID jobId;
        Instant start;
        Instant end;
    }
}
