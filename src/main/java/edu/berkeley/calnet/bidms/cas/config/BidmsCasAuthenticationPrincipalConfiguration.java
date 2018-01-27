/**
 * Derived from the CAS project's CasCoreAuthenticationPrincipalConfiguration.
 *
 * See https://github.com/apereo/cas/blob/master/LICENSE for licensing
 * information.
 */
package edu.berkeley.calnet.bidms.cas.config;

import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.principal.resolvers.PersonDirectoryPrincipalResolver;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Overrides {@link CasCoreAuthenticationPrincipalConfiguration} to remove
 * EchoingPrincipalResolver from the resolver chain because uid is our
 * principal, not the calnetId.
 */
@Configuration("bidmsCasAuthenticationPrincipalConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@AutoConfigureBefore(CasCoreAuthenticationPrincipalConfiguration.class)
public class BidmsCasAuthenticationPrincipalConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(BidmsCasAuthenticationPrincipalConfiguration.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("attributeRepositories")
    private List<IPersonAttributeDao> attributeRepositories;

    @Autowired
    @Qualifier("attributeRepository")
    private IPersonAttributeDao attributeRepository;

    @Autowired
    @RefreshScope
    @Bean
    @ConditionalOnMissingBean(name = "personDirectoryPrincipalResolver")
    public PrincipalResolver personDirectoryPrincipalResolver(@Qualifier("principalFactory") final PrincipalFactory principalFactory) {
        LOGGER.info("Using BidmsCasAuthenticationPrincipalConfiguration");

        final PersonDirectoryPrincipalResolver bean = new PersonDirectoryPrincipalResolver(
                attributeRepository,
                principalFactory,
                casProperties.getPersonDirectory().isReturnNull(),
                casProperties.getPersonDirectory().getPrincipalAttribute()
        );

        if (attributeRepositories.isEmpty()) {
            LOGGER.warn("Attribute repositories are empty.  This is a sign of a configuration problem");
        }

        return bean;
    }
}
