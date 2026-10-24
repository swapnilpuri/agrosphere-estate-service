package com.agrosphere.estate;
import java.util.*;
import java.time.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import tools.jackson.databind.*;
import com.agrosphere.estate.adapter.out.messaging.OutboxDispatcher;
import com.agrosphere.estate.application.*;
import com.agrosphere.estate.application.port.out.*;
import com.agrosphere.estate.domain.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EstateServiceIT {
    static PostgreSQLContainer postgres;
    static GenericContainer<?> broker;
    static HttpServer oidc;
    static RSAKey key;
    static String issuer,base;
    static ConfigurableApplicationContext context;
    static ObjectMapper json;
    static JdbcTemplate jdbc;
    static UUID estateId,divisionId,blockId;
    static HttpClient http=HttpClient.newHttpClient();
    @BeforeAll static void start() throws Exception {
        postgres=new PostgreSQLContainer("postgres:17-alpine");postgres.start();
        broker=new GenericContainer<>("rabbitmq:4.3.6-management").withExposedPorts(5672)
            .withEnv("RABBITMQ_DEFAULT_USER","integration").withEnv("RABBITMQ_DEFAULT_PASS","integration-test-only");broker.start();
        key=new RSAKeyGenerator(2048).keyID("integration-key").generate();
        oidc=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);issuer="http://127.0.0.1:"+oidc.getAddress().getPort();
        oidc.createContext("/.well-known/openid-configuration",exchange->{byte[] body=("{\"issuer\":\""+issuer+"\",\"jwks_uri\":\""+issuer+"/jwks\",\"id_token_signing_alg_values_supported\":[\"RS256\"]}").getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});
        oidc.createContext("/jwks",exchange->{byte[] body=new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});oidc.start();
        context=SpringApplication.run(EstateServiceApplication.class,
            "--server.port=0","--spring.datasource.url="+postgres.getJdbcUrl(),"--spring.datasource.username="+postgres.getUsername(),"--spring.datasource.password="+postgres.getPassword(),
            "--spring.security.oauth2.resourceserver.jwt.issuer-uri="+issuer,"--estate.security.audience=estate-service","--estate.data-provenance=SIMULATED","--estate.outbox.enabled=false","--management.otlp.metrics.export.enabled=false","--management.tracing.export.otlp.enabled=false",
            "--spring.rabbitmq.host="+broker.getHost(),"--spring.rabbitmq.port="+broker.getMappedPort(5672),"--spring.rabbitmq.username=integration","--spring.rabbitmq.password=integration-test-only");
        base="http://localhost:"+((WebServerApplicationContext)context).getWebServer().getPort();json=context.getBean(ObjectMapper.class);jdbc=context.getBean(JdbcTemplate.class);
    }
    @AfterAll static void stop() { if(context!=null)context.close();if(oidc!=null)oidc.stop(0);if(broker!=null)broker.stop();if(postgres!=null)postgres.stop(); }
    static String token(String role,List<String> estates,String audience,Instant expires) throws Exception { return token(role,estates,audience,expires,issuer); }
    static String token(String role,List<String> estates,String audience,Instant expires,String tokenIssuer) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(tokenIssuer).subject("integration-user").audience(audience).issueTime(Date.from(Instant.now().minusSeconds(5))).expirationTime(Date.from(expires))
            .claim("realm_access",Map.of("roles",List.of(role))).claim("estate_ids",estates).build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),claims);jwt.sign(new RSASSASigner(key));return jwt.serialize();
    }
    static String admin() throws Exception { return token("PLATFORM_ADMIN",List.of(),"estate-service",Instant.now().plusSeconds(300)); }
    static HttpResponse<String> request(String method,String uri,Object body,String token) throws Exception {
        var r=HttpRequest.newBuilder(URI.create(base+uri)).header("Content-Type","application/json").header("X-Correlation-ID","00000000-0000-0000-0000-000000000099");
        if(token!=null)r.header("Authorization","Bearer "+token);
        return http.send(r.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    static JsonNode body(HttpResponse<String> response,int status) { assertEquals(status,response.statusCode(),response.body());return json.readTree(response.body()); }
    @Test @Order(1) void migrationsAndEstateCrud() throws Exception {
        assertEquals(3,jdbc.queryForObject("select count(*) from flyway_schema_history where success=true",Integer.class));
        var data=Map.of("estateCode","e1","name","Rubber Estate","countryCode","LK","timezone","Asia/Colombo","status","ACTIVE");
        var response=request("POST","/api/v1/estates",data,admin());var estate=body(response,201);estateId=UUID.fromString(estate.get("id").asText());
        assertEquals("00000000-0000-0000-0000-000000000099",response.headers().firstValue("X-Correlation-ID").orElseThrow());
        assertEquals("E1",body(request("GET","/api/v1/estates/"+estateId,null,admin()),200).get("estateCode").asText());
        body(request("POST","/api/v1/estates",data,admin()),409);
        assertEquals(1,jdbc.queryForObject("select count(*) from outbox_event",Integer.class));
        body(request("PATCH","/api/v1/estates/"+estateId+"/status",Map.of("status","INACTIVE","version",0),admin()),200);
        body(request("PATCH","/api/v1/estates/"+estateId+"/status",Map.of("status","ACTIVE","version",0),admin()),409);
        body(request("PATCH","/api/v1/estates/"+estateId+"/status",Map.of("status","ACTIVE","version",1),admin()),200);
        var update=new LinkedHashMap<String,Object>(data);update.put("name","Updated estate");update.put("version",2);body(request("PUT","/api/v1/estates/"+estateId,update,admin()),200);
    }
    @Test @Order(2) void divisionsClonesBlocksAndConstraints() throws Exception {
        var division=body(request("POST","/api/v1/estates/"+estateId+"/divisions",Map.of("estateId",estateId,"divisionCode","D1","name","North","status","ACTIVE"),admin()),201);divisionId=UUID.fromString(division.get("id").asText());
        body(request("POST","/api/v1/rubber-clones",Map.of("cloneCode","RRIC100","description","Rubber clone","status","ACTIVE"),admin()),201);
        body(request("POST","/api/v1/rubber-clones",Map.of("cloneCode","rric100","description","Duplicate clone","status","ACTIVE"),admin()),409);
        body(request("POST","/api/v1/estates/"+estateId+"/divisions",Map.of("estateId",estateId,"divisionCode","d1","name","Duplicate","status","ACTIVE"),admin()),409);
        body(request("GET","/api/v1/divisions/"+divisionId,null,admin()),200);
        body(request("PUT","/api/v1/divisions/"+divisionId,Map.of("estateId",estateId,"divisionCode","D1","name","North Updated","status","ACTIVE","version",0),admin()),200);
        assertEquals(1,body(request("GET","/api/v1/estates/"+estateId+"/divisions",null,admin()),200).get("totalElements").asInt());
        assertEquals(1,body(request("GET","/api/v1/rubber-clones",null,admin()),200).get("totalElements").asInt());
        var block=new LinkedHashMap<String,Object>(Map.of("estateId",estateId,"divisionId",divisionId,"blockCode","B1","name","Block One","cropType","RUBBER","rubberCloneCode","RRIC100","plantedAreaHectares",2.5,"plantedTreeCount",100,"tappableTreeCount",70,"status","ACTIVE"));
        blockId=UUID.fromString(body(request("POST","/api/v1/divisions/"+divisionId+"/blocks",block,admin()),201).get("id").asText());
        body(request("POST","/api/v1/divisions/"+divisionId+"/blocks",block,admin()),409);
        block.put("version",0);block.put("name","Updated block");body(request("PUT","/api/v1/blocks/"+blockId,block,admin()),200);block.remove("version");
        block.put("blockCode","B2");block.put("tappableTreeCount",101);body(request("POST","/api/v1/divisions/"+divisionId+"/blocks",block,admin()),400);
        assertEquals(1,body(request("GET","/api/v1/estates/"+estateId+"/blocks?status=ACTIVE&page=0&size=10",null,admin()),200).get("totalElements").asInt());
        body(request("PATCH","/api/v1/blocks/"+blockId+"/status",Map.of("status","INACTIVE","version",1),admin()),200);
        var view=body(request("GET","/api/v1/blocks/"+blockId,null,admin()),200);assertEquals("INACTIVE",view.get("status").asText());
    }
    @Test @Order(6) void actualConcurrentUpdatesUseJpaOptimisticLocking() {
        var emf=context.getBean(jakarta.persistence.EntityManagerFactory.class);
        var first=emf.createEntityManager();var second=emf.createEntityManager();
        try {
            first.getTransaction().begin();second.getTransaction().begin();
            var a=first.find(com.agrosphere.estate.adapter.out.persistence.EstateEntity.class,estateId);
            var b=second.find(com.agrosphere.estate.adapter.out.persistence.EstateEntity.class,estateId);
            Instant created=a.createdAt;long version=a.version;
            a.name="Concurrent winner";a.updatedAt=Instant.now();first.getTransaction().commit();
            assertEquals(version+1,a.version);assertEquals(created,a.createdAt);
            b.name="Stale writer";b.updatedAt=Instant.now();
            assertThrows(jakarta.persistence.OptimisticLockException.class,second::flush);
        } finally { if(first.getTransaction().isActive())first.getTransaction().rollback();if(second.getTransaction().isActive())second.getTransaction().rollback();first.close();second.close(); }
    }
    @Test @Order(7) void migrationUpgradePreservesPublishedHistory() {
        var flyway=org.flywaydb.core.Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).schemas("upgrade_check").defaultSchema("upgrade_check").target("1").load();flyway.migrate();
        UUID id=UUID.fromString("00000000-0000-0000-0000-000000000055");
        jdbc.update("insert into upgrade_check.outbox_event(event_id,aggregate_id,event_type,event_version,correlation_id,occurred_at,payload,published_at) values (?,?,?,1,?,now(),'{}'::jsonb,now())",id,id,"EstateCreated",id);
        org.flywaydb.core.Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).schemas("upgrade_check").defaultSchema("upgrade_check").load().migrate();
        assertEquals("PUBLISHED",jdbc.queryForObject("select delivery_status from upgrade_check.outbox_event where event_id=?",String.class,id));
    }
    @Test @Order(3) void authorizationAndErrors() throws Exception {
        body(request("GET","/api/v1/estates",null,null),401);
        String viewer=token("VIEWER",List.of(estateId.toString()),"estate-service",Instant.now().plusSeconds(300));
        body(request("GET","/api/v1/estates/"+estateId,null,viewer),200);
        body(request("PATCH","/api/v1/blocks/"+blockId+"/status",Map.of("status","ACTIVE","version",1),viewer),403);
        String outsider=token("ESTATE_MANAGER",List.of(DomainRulesTest.DIVISION.toString()),"estate-service",Instant.now().plusSeconds(300));
        body(request("GET","/api/v1/blocks/"+blockId,null,outsider),403);
        assertEquals(0,body(request("GET","/api/v1/estates",null,outsider),200).get("totalElements").asInt());
        body(request("GET","/api/v1/estates",null,token("PLATFORM_ADMIN",List.of(),"wrong-audience",Instant.now().plusSeconds(300))),401);
        body(request("GET","/api/v1/estates",null,token("PLATFORM_ADMIN",List.of(),"estate-service",Instant.now().minusSeconds(120))),401);
        body(request("GET","/api/v1/estates",null,token("PLATFORM_ADMIN",List.of(),"estate-service",Instant.now().plusSeconds(300),"https://untrusted.invalid")),401);
        var forged=SignedJWT.parse(admin());forged=new SignedJWT(forged.getHeader(),forged.getJWTClaimsSet());forged.sign(new RSASSASigner(new RSAKeyGenerator(2048).generate()));
        body(request("GET","/api/v1/estates",null,forged.serialize()),401);
        body(request("GET","/api/v1/blocks/not-a-uuid",null,admin()),400);
        body(request("GET","/api/v1/blocks/00000000-0000-0000-0000-000000000088",null,admin()),404);
    }
    @Test @Order(8) void databaseRejectsCrossEstateDivisionReferences() throws Exception {
        UUID other=UUID.fromString(body(request("POST","/api/v1/estates",Map.of("estateCode","E2","name","Other","countryCode","LK","timezone","Asia/Colombo","status","ACTIVE"),admin()),201).get("id").asText());
        var bad=Map.of("estateId",other,"divisionId",divisionId,"blockCode","BAD","name","Bad","cropType","RUBBER","plantedAreaHectares",1,"plantedTreeCount",1,"tappableTreeCount",1,"status","ACTIVE");
        body(request("POST","/api/v1/divisions/"+divisionId+"/blocks",bad,admin()),400);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("insert into plantation_block(id,estate_id,division_id,block_code,name,crop_type,planted_area_hectares,planted_tree_count,tappable_tree_count,status,created_at,updated_at) values (?,?,?,'SQL_BAD','Bad','RUBBER',1,1,1,'ACTIVE',now(),now())",UUID.fromString("00000000-0000-0000-0000-000000000044"),other,divisionId));
    }
    @Test @Order(9) void healthAndGeneratedApiDocumentation() throws Exception {
        body(request("GET","/actuator/health",null,null),200);
        body(request("GET","/actuator/health/liveness",null,null),200);
        body(request("GET","/actuator/health/readiness",null,null),200);
        body(request("GET","/v3/api-docs",null,null),401);
        var response=request("GET","/v3/api-docs",null,admin());var api=body(response,200);
        assertTrue(api.get("paths").has("/api/v1/estates"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target","openapi-runtime.json"),response.body());
    }
    @Test @Order(4) void domainAndOutboxRollbackTogether() {
        int before=jdbc.queryForObject("select count(*) from outbox_event",Integer.class);
        UUID id=UUID.fromString("00000000-0000-0000-0000-000000000077");
        var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{var now=Instant.EPOCH;
            context.getBean(EstateStore.class).saveEstate(new Estate(id,"ROLLBACK","Rollback","LK","Asia/Colombo",null,Status.ACTIVE,now,now,null));
            context.getBean(EventOutbox.class).append("EstateCreated",id,id,Map.of("name","Rollback"));throw new IllegalStateException("force rollback"); }));
        assertEquals(0,jdbc.queryForObject("select count(*) from estate where id=?",Integer.class,id));assertEquals(before,jdbc.queryForObject("select count(*) from outbox_event",Integer.class));
    }
    @Test @Order(5) void publisherConfirmsAndDuplicateRedelivery() throws Exception {
        var exports=java.nio.file.Path.of("target","event-contract-samples");java.nio.file.Files.createDirectories(exports);
        for(var row:jdbc.queryForList("select event_id,payload::text as envelope from outbox_event")) java.nio.file.Files.writeString(exports.resolve(row.get("event_id")+".json"),row.get("envelope").toString());
        var rabbit=context.getBean(RabbitTemplate.class);var admin=new RabbitAdmin(rabbit);admin.declareExchange(new TopicExchange("agrosphere.domain",true,false));
        var queue=new org.springframework.amqp.core.Queue("estate-integration-events",true);admin.declareQueue(queue);admin.declareBinding(BindingBuilder.bind(queue).to(new TopicExchange("agrosphere.domain")).with("agrosphere.*.v2"));
        var dispatcher=new OutboxDispatcher(jdbc,rabbit,3,100,1,5);var tx=new TransactionTemplate(context.getBean(PlatformTransactionManager.class));tx.executeWithoutResult(status->dispatcher.dispatch());
        assertEquals(0,jdbc.queryForObject("select count(*) from outbox_event where delivery_status<>'PUBLISHED'",Integer.class));
        var first=rabbit.receive(queue.getName(),5000);assertNotNull(first);String id=first.getMessageProperties().getMessageId();
        var envelope=json.readTree(first.getBody());assertEquals(2,envelope.get("eventVersion").asInt());assertEquals("estate-service",envelope.get("sourceService").asText());assertNotNull(envelope.get("correlationId"));
        while(rabbit.receive(queue.getName(),100)!=null) {}
        jdbc.update("update outbox_event set delivery_status='PENDING',published_at=null,next_attempt_at=now() where event_id=?",UUID.fromString(id));tx.executeWithoutResult(status->dispatcher.dispatch());
        var duplicate=rabbit.receive(queue.getName(),5000);assertNotNull(duplicate);assertEquals(id,duplicate.getMessageProperties().getMessageId());
    }
}
