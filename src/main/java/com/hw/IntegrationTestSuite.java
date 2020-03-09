package com.hw;

import com.hw.concurrent.ProductServiceTest;
import com.hw.integration.oauth2.*;
import com.hw.integration.product.CategoryTest;
import com.hw.integration.product.ProductTest;
import com.hw.integration.profile.AddressTest;
import com.hw.integration.profile.CartTest;
import com.hw.integration.profile.OrderTest;
import com.hw.integration.proxy.BlackListControllerTest;
import com.hw.integration.proxy.CORSTest;
import com.hw.integration.proxy.ClientEPSecurityTest;
import com.hw.integration.proxy.SecurityProfileControllerTest;
import com.hw.integration.security.SecurityTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(value = {
        ProductServiceTest.class,
        AuthorizationCodeTest.class,
        ClientControllerTest.class,
        ClientCredentialsTest.class,
        PasswordFlowTest.class,
        ResourceOwnerControllerTest.class,
        CategoryTest.class,
        ProductTest.class,
        AddressTest.class,
        CartTest.class,
        OrderTest.class,
        BlackListControllerTest.class,
        ClientEPSecurityTest.class,
        CORSTest.class,
        ResourceOwnerControllerTest.class,
        SecurityProfileControllerTest.class,
        SecurityTest.class

})
public class IntegrationTestSuite {
}
