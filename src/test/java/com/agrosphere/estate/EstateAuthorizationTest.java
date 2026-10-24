package com.agrosphere.estate;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.agrosphere.estate.adapter.out.security.JwtEstateAccess;
class EstateAuthorizationTest {
    JwtEstateAccess access=new JwtEstateAccess(); UUID estate=DomainRulesTest.ESTATE;
    void login(String role,Object ids) { var jwt=Jwt.withTokenValue("test").header("alg","RS256").claim("sub","test-user").claim("estate_ids",ids).build(); SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_"+role)))); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void viewerCanReadAssignedEstate() { login("VIEWER",List.of(estate.toString()));assertDoesNotThrow(()->access.read(estate)); }
    @Test void viewerCannotWrite() { login("VIEWER",List.of(estate.toString()));assertThrows(AccessDeniedException.class,()->access.write(estate)); }
    @Test void managerCannotReadOrWriteAnotherEstate() { login("ESTATE_MANAGER",List.of(estate.toString()));assertThrows(AccessDeniedException.class,()->access.read(DomainRulesTest.DIVISION));assertThrows(AccessDeniedException.class,()->access.write(DomainRulesTest.DIVISION)); }
    @Test void managerCanWriteAssignedEstate() { login("ESTATE_MANAGER",List.of(estate.toString()));assertDoesNotThrow(()->access.write(estate)); }
    @Test void malformedScopeIsDenied() { login("ESTATE_ADMIN",List.of("not-uuid"));assertThrows(AccessDeniedException.class,()->access.read(estate)); }
    @Test void arbitraryRoleIsDenied() { login("other",List.of(estate.toString()));assertThrows(AccessDeniedException.class,()->access.read(estate)); }
    @Test void platformAdministratorCanAccessAllEstates() { login("PLATFORM_ADMIN",List.of());assertNull(access.readableEstates());assertDoesNotThrow(()->access.write(estate)); }
    @Test void managerCannotCreateEstateOrClone() { login("ESTATE_MANAGER",List.of(estate.toString()));assertThrows(AccessDeniedException.class,access::platformAdmin); }
}
