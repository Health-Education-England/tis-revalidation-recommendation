package uk.nhs.hee.tis.revalidation.config;

import static java.lang.String.format;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;


@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private static final String ADMIN_ROLE = "ADMIN";

  @Value("${app.gmc.authUser}")
  private String userName;

  @Value("${app.gmc.authPassword}")
  private String password;

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    http.csrf().disable()
        .authorizeRequests()
        .antMatchers("/api/admin/**").authenticated()
        .and()
        .httpBasic();
  }

  @Autowired
  public void configureGlobal(final AuthenticationManagerBuilder auth)
      throws Exception {
    final String passwordWithEncoding = format("{noop}%s", password);
    auth.inMemoryAuthentication()
        .withUser(userName)
        .password(passwordWithEncoding)
        .roles(ADMIN_ROLE);
  }
}