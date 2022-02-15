package kurstest

import kotliquery.sessionOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import javax.sql.DataSource

@Configuration
@EnableWebSecurity
open class KursSecurityConfig : WebSecurityConfigurerAdapter() {
    @Autowired
    lateinit var dataSource: DataSource
    @Autowired
    lateinit var roleHierarchy: RoleHierarchy
    @Autowired
    lateinit var rememberMeKey: String

    @Autowired
    fun configureGlobal(authenticationManagerBuilder: AuthenticationManagerBuilder) {
        authenticationManagerBuilder.authenticationProvider(object : AuthenticationProvider {
            override fun authenticate(auth: Authentication): Authentication? {
                val username = auth.principal as String
                val password = auth.credentials as String

                val isVerified = sessionOf(dataSource).use { dbSess -> verifyUserPassword(dbSess, username, password)}

                if (isVerified) {
                    return UsernamePasswordAuthenticationToken(username, password, listOf(SimpleGrantedAuthority("ROLE_USER")))
                }

                if (username == "quentin" && password == "test") {
                    return UsernamePasswordAuthenticationToken(username, password, listOf(SimpleGrantedAuthority("ROLE_USER")))
                }

                if (username == "admin" && password == "test") {
                    return UsernamePasswordAuthenticationToken(username, password, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
                }

                return null
            }

            override fun supports(authentication: Class<*>) = authentication == UsernamePasswordAuthenticationToken::class.java
        })
    }

    override fun configure(web: WebSecurity) {
        web.ignoring()
            .antMatchers("/assets/**")
    }

    override fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
            .expressionHandler(DefaultWebSecurityExpressionHandler().apply { setRoleHierarchy(roleHierarchy) })
            .antMatchers("/debug/userdetails").permitAll()
            .antMatchers("/login").permitAll()
            .antMatchers("/").permitAll()
            .antMatchers("/about").permitAll()
            .antMatchers("/admin/**").hasRole("ADMIN")
            .antMatchers("/**").hasRole("USER")
            .anyRequest().authenticated()
            .and().formLogin()
            .and().rememberMe().key(rememberMeKey).rememberMeServices(TokenBasedRememberMeServices(rememberMeKey, userDetailsService()).apply {
                setCookieName("REMEMBER_ME_KURSDEMO")
            })
            .and().logout().logoutRequestMatcher(AntPathRequestMatcher("/logout"))
    }
}