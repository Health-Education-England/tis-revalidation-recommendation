package uk.nhs.hee.tis.revalidation.changelog;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.YES;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@ExtendWith(MockitoExtension.class)
class SetDicsonnectedDoctorsUnderNoticeToNullChangeLogTest {

    SetDisconnectedDoctorsUnderNoticeToNullChangeLog changeLog;

    @Mock
    DoctorsForDBRepository doctorsForDBRepository;

    @Captor
    ArgumentCaptor<DoctorsForDB> doctorCaptor;

    List<DoctorsForDB> doctors;

    DoctorsForDB doctor1;

    private final Faker faker = new Faker();

    @BeforeEach
    public void setup() {
        changeLog = new SetDisconnectedDoctorsUnderNoticeToNullChangeLog();
        setupTestData();
    }

    @Test
    void shouldSetCorrectTisStatusForEachDoctor() {

        assert (doctor1.getDoctorStatus()).equals(RecommendationStatus.NOT_STARTED);
        when(doctorsForDBRepository.findByExistsInGmcIsFalse()).thenReturn(doctors);

        changeLog.setDisconnectedDoctorsUnderNoticeToNull(
            doctorsForDBRepository
        );
        verify(doctorsForDBRepository).save(doctorCaptor.capture());
        assertNull (doctorCaptor.getValue().getUnderNotice());
    }

    private void setupTestData() {
        doctor1 = DoctorsForDB.builder()
            .gmcReferenceNumber(faker.idNumber().toString())
            .doctorFirstName(faker.name().firstName())
            .doctorLastName(faker.name().lastName())
            .submissionDate(LocalDate.now())
            .dateAdded(LocalDate.now())
            .underNotice(YES)
            .sanction(faker.lorem().fixedString(5))
            .doctorStatus(RecommendationStatus.NOT_STARTED)
            .lastUpdatedDate(LocalDate.now())
            .designatedBodyCode(faker.lorem().fixedString(5))
            .existsInGmc(false)
            .build();
        doctors = List.of(doctor1);
    }
}
