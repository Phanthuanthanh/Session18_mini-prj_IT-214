package com.rikkeibank.customer.config;

import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.CustomerRepository;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerDataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    @Override
    public void run(String... args) {
        if (staffRepository.count() == 0) {
            log.info("Seeding initial staff...");
            staffRepository.save(Staff.builder()
                    .userId(2L) // Maps to teller1 in identity-service
                    .staffCode("TEL-001")
                    .fullName("Lê Văn Cường (Giao dịch viên 1)")
                    .email("teller1@rikkeibank.com")
                    .phone("0987654321")
                    .branch("RikkeiBank Chi nhánh Cầu Giấy")
                    .department("Phòng Dịch vụ Khách hàng")
                    .status("ACTIVE")
                    .build());
        }

        if (customerRepository.count() == 0) {
            log.info("Seeding initial customers...");
            customerRepository.save(Customer.builder()
                    .userId(3L) // Maps to customer1 in identity-service
                    .fullName("Nguyễn Văn An")
                    .email("customer1@rikkeibank.com")
                    .phone("0912345678")
                    .identityCardNumber("001200000001")
                    .address("Tòa nhà Handico, Nam Từ Liêm, Hà Nội")
                    .dateOfBirth(LocalDate.of(1995, 5, 20))
                    .status("ACTIVE")
                    .assignedTellerId(1L)
                    .build());

            customerRepository.save(Customer.builder()
                    .userId(4L) // Maps to customer2 in identity-service
                    .fullName("Trần Thị Bình")
                    .email("customer2@rikkeibank.com")
                    .phone("0912345679")
                    .identityCardNumber("001200000002")
                    .address("Tòa nhà Keangnam, Cầu Giấy, Hà Nội")
                    .dateOfBirth(LocalDate.of(1998, 10, 15))
                    .status("ACTIVE")
                    .assignedTellerId(1L)
                    .build());

            log.info("Initialized default customers: Nguyễn Văn An, Trần Thị Bình");
        }
    }
}
