package com.pplflw.challenge;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeAddEventDto;
import com.pplflw.challenge.dto.EmployeeChangeStateEventDto;
import com.pplflw.challenge.service.dto.InputEmployeeDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import com.pplflw.challenge.statemachine.EmployeeState;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestApiIntegrationTests {

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    KafkaProperties kafkaProperties;

    @Value("${com.pplflw.challenge.kafka.employee-add-topic}")
    private String addEmployeeTopic;

    @Value("${com.pplflw.challenge.kafka.employee-change-state-topic}")
    private String changeEmployeeStateTopic;

    @BeforeAll
    static void start() {
        kafka.start();
        System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());
    }

    @AfterAll
    static void afterAll() {
        kafka.stop();
    }

    @Test
    public void testAddEmployee() {

        KafkaConsumer<String, EmployeeAddEventDto> consumer = consumer();

        consumer.subscribe(Collections.singletonList(addEmployeeTopic));

        InputEmployeeDto inputEmployeeDto = createTestInputEmployeeDto();

        restTemplate.exchange("/employees/add",
                HttpMethod.POST,
                new HttpEntity<>(inputEmployeeDto),
                Employee.class);

        Unreliables.retryUntilTrue(10, TimeUnit.SECONDS, () -> {
            ConsumerRecords<String, EmployeeAddEventDto> records = consumer.poll(Duration.ofMillis(100));

            if (records.isEmpty()) {
                return false;
            }

            assertThat(records).hasSize(1)
                    .extracting(ConsumerRecord::topic, ConsumerRecord::value)
                    .containsExactly(tuple(addEmployeeTopic, new EmployeeAddEventDto(createTestEmployee())));

            return true;
        });

        consumer.unsubscribe();
    }

    @Test
    public void testMultipleChangeState() {

        KafkaConsumer<String, EmployeeChangeStateEventDto> consumer = consumer();

        consumer.subscribe(Collections.singletonList(changeEmployeeStateTopic));

        Employee employee = createTestEmployee();

        restTemplate.exchange("/employees/{id}/check",
                HttpMethod.PUT,
                null,
                EmployeeChangeStateEventDto.class,
                employee.getId());
        restTemplate.exchange("/employees/{id}/reject",
                HttpMethod.PUT,
                null,
                EmployeeChangeStateEventDto.class,
                employee.getId());
        restTemplate.exchange("/employees/{id}/approve",
                HttpMethod.PUT,
                null,
                EmployeeChangeStateEventDto.class,
                employee.getId());
        restTemplate.exchange("/employees/{id}/activate",
                HttpMethod.PUT,
                null,
                EmployeeChangeStateEventDto.class,
                employee.getId());

        Unreliables.retryUntilTrue(10, TimeUnit.SECONDS, () -> {
            ConsumerRecords<String, EmployeeChangeStateEventDto> records = consumer.poll(Duration.ofMillis(100));

            if (records.isEmpty()) {
                return false;
            }

            assertThat(records).hasSize(4);
            assertThat(records).extracting(ConsumerRecord::topic).containsOnly(changeEmployeeStateTopic);
            assertThat(records).extracting(r -> r.value().getEmployeeId()).containsOnly(employee.getId());
            assertThat(records).extracting(r -> r.value().getEvent())
                    .containsExactly(EmployeeEvent.CHECK,
                            EmployeeEvent.REJECT,
                            EmployeeEvent.APPROVE,
                            EmployeeEvent.ACTIVATE);

            return true;
        });

        consumer.unsubscribe();
    }

    private <V> KafkaConsumer<String, V> consumer() {

        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();

        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());

        return new KafkaConsumer<>(consumerProperties);
    }

    private Employee createTestEmployee() {
        return new Employee(1L, EmployeeState.ADDED, "Aliaksei Protas", "Washington Capitals prospect", 20);
    }

    private InputEmployeeDto createTestInputEmployeeDto() {
        return new InputEmployeeDto("Aliaksei Protas", "Washington Capitals prospect", 20);
    }
}