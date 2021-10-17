package com.zenjob.challenge.service;

import com.zenjob.challenge.entity.Job;
import com.zenjob.challenge.entity.Shift;
import com.zenjob.challenge.repository.JobRepository;
import com.zenjob.challenge.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@RequiredArgsConstructor
@Repository
@Transactional
public class JobService {
    private final JobRepository jobRepository;
    private final ShiftRepository shiftRepository;

    public Job createJob(UUID uuid, LocalDate date1, LocalDate date2) {
        Job job = Job.builder()
                .id(uuid)
                .companyId(UUID.randomUUID())
                .startTime(date1.atTime(8, 0, 0).toInstant(ZoneOffset.UTC))
                .endTime(date2.atTime(17, 0, 0).toInstant(ZoneOffset.UTC))
                .build();
        job.setShifts(LongStream.range(0, ChronoUnit.DAYS.between(date1, date2))
                .mapToObj(idx -> date1.plus(idx, ChronoUnit.DAYS))
                .map(date -> Shift.builder()
                        .id(UUID.randomUUID())
                        .job(job)
                        .startTime(date.atTime(8, 0, 0).toInstant(ZoneOffset.UTC))
                        .endTime(date.atTime(17, 0, 0).toInstant(ZoneOffset.UTC))
                        .build())
                .collect(Collectors.toList()));
        return jobRepository.save(job);
    }

    public List<Shift> getShifts(UUID id) {
        return shiftRepository.findAllByJob_Id(id);
    }

    public void bookTalent(UUID talent, UUID shiftId) {
        shiftRepository.findById(shiftId).map(shift -> shiftRepository.save(shift.setTalentId(talent)));
    }

    public void replaceShift(UUID talent, UUID shiftId) {
        Shift newShift = new Shift();
        Optional<Shift> optionalShift = shiftRepository.findById(shiftId);

        if (optionalShift.isPresent()) {
            Shift oldShift = optionalShift.get();
            // Will assume copying the info from previous Shift

            newShift.setId(UUID.randomUUID());
            newShift.setJob(oldShift.getJob());
            newShift.setTalentId(talent);
            newShift.setStartTime(oldShift.getStartTime());
            newShift.setEndTime(oldShift.getEndTime());

            // Delete previous shift
            shiftRepository.deleteById(shiftId);
            // Save new shift
            shiftRepository.save(newShift);
        }
    }

    // Find all shifts by Example
    public List<Shift> getShiftsPerTalent(UUID talent) {
        Shift shiftTalent = new Shift();
        shiftTalent.setTalentId(talent);
        Example<Shift> shiftExample = Example.of(shiftTalent);
        return shiftRepository.findAll(shiftExample);

    }

    public Optional<Job> getJobs(UUID id) {
        return jobRepository.findById(id);
    }

    // Will find shift per shiftID and remove it
    public void deleteShift(UUID shiftID) {
        shiftRepository.deleteById(shiftID);
    }

    // Will remove job
    public void deleteJob(UUID jobId) {
        // Cascade & Orphan Removal on job to shift
        jobRepository.deleteById(jobId);
    }


}
