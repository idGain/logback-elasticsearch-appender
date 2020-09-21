package de.cgoit.logback.elasticsearch.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BasicAuthentication.class})
public class BasicAuthenticationTest {

    private static final String ENV_VAR_SET_NAME = "ENV_VAR_SET";
    private static final String ENV_VAR_SET_KEY = "${env." + ENV_VAR_SET_NAME + "}";
    private static final String ENV_VAR_SET_VALUE = "ThisIsSet";
    private static final String ENV_VAR_NOT_SET_NAME = "NOT_SET";
    private static final String ENV_VAR_NOT_SET_KEY = "${env." + ENV_VAR_NOT_SET_NAME + "}";

    @Before
    public void setup() {
        PowerMockito.mockStatic(BasicAuthentication.class);
        PowerMockito.when(BasicAuthentication.getFromEnv(Mockito.eq(ENV_VAR_SET_NAME))).thenReturn(ENV_VAR_SET_VALUE);
        PowerMockito.when(BasicAuthentication.getFromEnv(Mockito.eq(ENV_VAR_NOT_SET_NAME))).thenReturn(null);
    }

    @Test
    public void resolve_env_var_if_env_var_is_set() {
        BasicAuthentication auth = new BasicAuthentication("TheUsername", ENV_VAR_SET_KEY);

        verifyStatic(BasicAuthentication.class, times(1));
        BasicAuthentication.getFromEnv(ENV_VAR_SET_NAME);
        assertEquals("Basic " + new String(Base64.getEncoder().encode(String.format("%s:%s", "TheUsername", ENV_VAR_SET_VALUE).getBytes())),
                Whitebox.getInternalState(auth, "authentication"));
    }

    @Test
    public void return_unresolved_env_var_if_env_var_is_not_set() {
        BasicAuthentication auth = new BasicAuthentication("TheUsername", ENV_VAR_NOT_SET_KEY);

        verifyStatic(BasicAuthentication.class, times(1));
        BasicAuthentication.getFromEnv(ENV_VAR_NOT_SET_NAME);
        assertEquals("Basic " + new String(Base64.getEncoder().encode(String.format("%s:%s", "TheUsername", ENV_VAR_NOT_SET_KEY).getBytes())),
                Whitebox.getInternalState(auth, "authentication"));
    }

    @Test
    public void return_unresolved_if_no_env_var() {
        BasicAuthentication auth = new BasicAuthentication("TheUsername", "ThePassword");

        verifyStatic(BasicAuthentication.class, times(0));
        BasicAuthentication.getFromEnv(any());
        assertEquals("Basic " + new String(Base64.getEncoder().encode(String.format("%s:%s", "TheUsername", "ThePassword").getBytes())),
                Whitebox.getInternalState(auth, "authentication"));
    }
}
