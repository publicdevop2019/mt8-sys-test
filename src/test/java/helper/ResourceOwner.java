package helper;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ResourceOwner {

    protected Long id;

    private String email;

    private String password;

    private Boolean locked;

    private List<GrantedAuthorityImpl<ResourceOwnerAuthorityEnum>> grantedAuthorities;

    private Set<String> resourceId;

}
