package com.pplflw.challenge;

import com.pplflw.challenge.domain.Employee;
import com.pplflw.challenge.dto.EmployeeAddEventDto;
import com.pplflw.challenge.dto.EmployeeChangeStateEventDto;
import com.pplflw.challenge.dto.EmployeeStatusEventDto;
import com.pplflw.challenge.statemachine.EmployeeEvent;
import com.pplflw.challenge.statemachine.EmployeeState;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.util.Lists;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceIntegrationTests {

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    @Autowired
    KafkaProperties kafkaProperties;

    @Value("${com.pplflw.challenge.kafka.employee-add-topic}")
    private String addEmployeeTopic;

    @Value("${com.pplflw.challenge.kafka.employee-change-state-topic}")
    private String changeEmployeeStateTopic;

    @Value("${com.pplflw.challenge.kafka.employee-status-topic}")
    private String employeeStatusTopic;

    @BeforeAll
    static void start() {
        kafka.start();
        System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());
    }

    @AfterAll
    static void afterAll() {
        kafka.stop();
    }

    /**
     * Tests both cases in one method to save a time and reduce code complexity.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testAddEmployeeAndMultipleChangeState() throws ExecutionException, InterruptedException {

        AdminClient adminClient = AdminClient.create(ImmutableMap.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()
        ));

        adminClient.createTopics(Lists.list(
                new NewTopic(addEmployeeTopic, 1, (short) 1),
                new NewTopic(changeEmployeeStateTopic, 1, (short) 1),
                new NewTopic(employeeStatusTopic, 1, (short) 1)
        ));

        Employee employee = createTestEmployee();

        KafkaConsumer<String, EmployeeStatusEventDto> consumer = consumer();
        consumer.subscribe(Collections.singletonList(employeeStatusTopic));

        KafkaProducer<String, EmployeeAddEventDto> addEmployeeEventProducer = producer();

        addEmployeeEventProducer.send(new ProducerRecord<>(addEmployeeTopic,
                new EmployeeAddEventDto(createTestEmployee()))).get();

        checkRecords(consumer, records -> {
            assertThat(records).hasSize(1)
                    .extracting(ConsumerRecord::topic, ConsumerRecord::value)
                    .containsExactly(tuple(employeeStatusTopic,
                            new EmployeeStatusEventDto(null, null, employee)));
        });

        KafkaProducer<String, EmployeeChangeStateEventDto> changeEmployeeStateProducer = producer();
        changeEmployeeStateProducer.send(new ProducerRecord<>(changeEmployeeStateTopic,
                new EmployeeChangeStateEventDto(employee.getId(), EmployeeEvent.CHECK))).get();

        checkRecords(consumer, records -> {

            assertThat(records).hasSize(1);
            assertThat(records).extracting(ConsumerRecord::topic).containsOnly(employeeStatusTopic);
            assertThat(records).extracting(r -> r.value().getEmployee().getState()).containsExactly(EmployeeState.IN_CHECK);
            assertThat(records).extracting(r -> r.value().getResult()).containsExactly("ACCEPTED");
            assertThat(records).extracting(r -> r.value().getEvent()).containsExactly(EmployeeEvent.CHECK);
        });

        changeEmployeeStateProducer.send(new ProducerRecord<>(changeEmployeeStateTopic,
                new EmployeeChangeStateEventDto(employee.getId(), EmployeeEvent.REJECT))).get();

        checkRecords(consumer, records -> {

            assertThat(records).hasSize(1);
            assertThat(records).extracting(ConsumerRecord::topic).containsOnly(employeeStatusTopic);
            assertThat(records).extracting(r -> r.value().getEmployee().getState()).containsExactly(EmployeeState.ADDED);
            assertThat(records).extracting(r -> r.value().getResult()).containsExactly("ACCEPTED");
            assertThat(records).extracting(r -> r.value().getEvent()).containsExactly(EmployeeEvent.REJECT);
        });

        changeEmployeeStateProducer.send(new ProducerRecord<>(changeEmployeeStateTopic,
                new EmployeeChangeStateEventDto(employee.getId(), EmployeeEvent.CHECK))).get();

        checkRecords(consumer, records -> {

            assertThat(records).hasSize(1);
            assertThat(records).extracting(ConsumerRecord::topic).containsOnly(employeeStatusTopic);
            assertThat(records).extracting(r -> r.value().getEmployee().getState()).containsExactly(EmployeeState.IN_CHECK);
            assertThat(records).extracting(r -> r.value().getResult()).containsExactly("ACCEPTED");
            assertThat(records).extracting(r -> r.value().getEvent()).containsExactly(EmployeeEvent.CHECK);
        });

        changeEmployeeStateProducer.send(new ProducerRecord<>(changeEmployeeStateTopic,
                new EmployeeChangeStateEventDto(employee.getId(), EmployeeEvent.ACTIVATE))).get();

        checkRecords(consumer, records -> {

            assertThat(records).hasSize(1);
            assertThat(records).extracting(ConsumerRecord::topic).containsOnly(employeeStatusTopic);
            assertThat(records).extracting(r -> r.value().getEmployee().getState()).containsExactly(EmployeeState.IN_CHECK);
            assertThat(records).extracting(r -> r.value().getResult()).containsExactly("DENIED");
            assertThat(records).extracting(r -> r.value().getEvent()).containsExactly(EmployeeEvent.ACTIVATE);
        });

        changeEmployeeStateProducer.send(new ProducerRecord<>(changeEmployeeStateTopic,
                new EmployeeChangeStateEventDto(employee.getId(), EmployeeEvent.APPROVE))).get();

        checkRecords(consumer, records -> {

            assertThat(records).hasSize(1);
            assertThat(records).extracting(ConsumerRecord::topic).containsOnly(employeeStatusTopic);
            assertThat(records).extracting(r -> r.value().getEmployee().getState()).containsExactly(EmployeeState.APPROVED);
            assertThat(records).extracting(r -> r.value().getResult()).containsExactly("ACCEPTED");
            assertThat(records).extracting(r -> r.value().getEvent()).containsExactly(EmployeeEvent.APPROVE);
        });

        changeEmployeeStateProducer.send(new ProducerRecord<>(changeEmployeeStateTopic,
                new EmployeeChangeStateEventDto(employee.getId(), EmployeeEvent.ACTIVATE))).get();

        checkRecords(consumer, records -> {

            assertThat(records).hasSize(1);
            assertThat(records).extracting(ConsumerRecord::topic).containsOnly(employeeStatusTopic);
            assertThat(records).extracting(r -> r.value().getEmployee().getState()).containsExactly(EmployeeState.ACTIVE);
            assertThat(records).extracting(r -> r.value().getResult()).containsExactly("ACCEPTED");
            assertThat(records).extracting(r -> r.value().getEvent()).containsExactly(EmployeeEvent.ACTIVATE);
        });

        consumer.unsubscribe();
    }

    private <V> KafkaProducer<String, V> producer() {

        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();

        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());

        return new KafkaProducer<>(producerProperties);
    }

    private <V> KafkaConsumer<String, V> consumer() {

        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();

        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());

        consumerProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.put("value.deserializer", "org.springframework.kafka.support.serializer.JsonDeserializer");

        return new KafkaConsumer<>(consumerProperties);
    }

    private <V> void checkRecords(KafkaConsumer<String, V> consumer, Consumer<ConsumerRecords<String, V>> asserts) {

        Unreliables.retryUntilTrue(10, TimeUnit.SECONDS, () -> {
            ConsumerRecords<String, V> records = consumer.poll(Duration.ofMillis(100));

            if (records.isEmpty()) {
                return false;
            }

            asserts.accept(records);

            return true;
        });
    }

    private Employee createTestEmployee() {
        return new Employee(1L, EmployeeState.ADDED, "Aliaksei Protas", "Washington Capitals prospect", 20);
    }
}