/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2021 The PWM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package password.pwm.http;

import lombok.Value;
import password.pwm.AppProperty;
import password.pwm.Permission;
import password.pwm.PwmApplicationMode;
import password.pwm.PwmConstants;
import password.pwm.PwmDomain;
import password.pwm.config.DomainConfig;
import password.pwm.config.PwmSetting;
import password.pwm.config.profile.HelpdeskProfile;
import password.pwm.config.profile.PeopleSearchProfile;
import password.pwm.config.profile.ProfileDefinition;
import password.pwm.error.PwmUnrecoverableException;
import password.pwm.http.servlet.PwmServletDefinition;
import password.pwm.ldap.UserInfo;
import password.pwm.util.java.StringUtil;
import password.pwm.util.java.TimeDuration;
import password.pwm.util.logging.PwmLogger;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class IdleTimeoutCalculator
{
    private static final PwmLogger LOGGER = PwmLogger.forClass( IdleTimeoutCalculator.class );

    public static MaxIdleTimeoutResult figureMaxSessionTimeout( final PwmDomain pwmDomain, final PwmSession pwmSession )
            throws PwmUnrecoverableException
    {
        final DomainConfig domainConfig = pwmDomain.getConfig();
        final SortedSet<MaxIdleTimeoutResult> results = new TreeSet<>();
        {
            final long idleSetting = domainConfig.readSettingAsLong( PwmSetting.IDLE_TIMEOUT_SECONDS );
            results.add( new MaxIdleTimeoutResult(
                    MaxIdleTimeoutResult.reasonFor( PwmSetting.IDLE_TIMEOUT_SECONDS, null ),
                    TimeDuration.of( idleSetting, TimeDuration.Unit.SECONDS ) ) );
        }

        if ( !pwmSession.isAuthenticated() )
        {
            if ( pwmDomain.getApplicationMode() == PwmApplicationMode.NEW )
            {
                final long configGuideIdleTimeout = Long.parseLong( domainConfig.readAppProperty( AppProperty.CONFIG_GUIDE_IDLE_TIMEOUT ) );
                results.add( new MaxIdleTimeoutResult(
                        "Configuration Guide Idle Timeout",
                        TimeDuration.of( configGuideIdleTimeout, TimeDuration.Unit.SECONDS ) ) );
            }

            if ( domainConfig.readSettingAsBoolean( PwmSetting.PEOPLE_SEARCH_ENABLE_PUBLIC ) )
            {
                final Optional<PeopleSearchProfile> optionalPeopleSearchProfile = domainConfig.getPublicPeopleSearchProfile();
                if ( optionalPeopleSearchProfile.isPresent() )
                {
                    final PeopleSearchProfile publicProfile = optionalPeopleSearchProfile.get();
                    final long peopleSearchIdleTimeout = publicProfile.readSettingAsLong( PwmSetting.PEOPLE_SEARCH_IDLE_TIMEOUT_SECONDS );
                    if ( peopleSearchIdleTimeout > 0 )
                    {
                        results.add( new MaxIdleTimeoutResult(
                                MaxIdleTimeoutResult.reasonFor( PwmSetting.PEOPLE_SEARCH_IDLE_TIMEOUT_SECONDS, publicProfile.getIdentifier() ),
                                TimeDuration.of( peopleSearchIdleTimeout, TimeDuration.Unit.SECONDS ) ) );
                    }
                }
            }

        }
        else
        {
            final UserInfo userInfo = pwmSession.getUserInfo();
            final boolean userIsAdmin = pwmSession.isAuthenticated()
                    && pwmSession.getSessionManager().checkPermission( pwmDomain, Permission.PWMADMIN );
            final Set<MaxIdleTimeoutResult> loggedInResults = figureMaxAuthUserTimeout( domainConfig, userInfo, userIsAdmin );
            results.addAll( loggedInResults );
        }

        return results.last();
    }

    private static Set<MaxIdleTimeoutResult> figureMaxAuthUserTimeout(
            final DomainConfig domainConfig,
            final UserInfo userInfo,
            final boolean userIsAdmin
    )
            throws PwmUnrecoverableException
    {
        final Set<MaxIdleTimeoutResult> results = new TreeSet<>();
        {
            final long idleSecondsSetting = domainConfig.readSettingAsLong( PwmSetting.IDLE_TIMEOUT_SECONDS );
            results.add( new MaxIdleTimeoutResult(
                    MaxIdleTimeoutResult.reasonFor( PwmSetting.IDLE_TIMEOUT_SECONDS, null ),
                    TimeDuration.of( idleSecondsSetting, TimeDuration.Unit.SECONDS ) ) );
        }

        if ( domainConfig.readSettingAsBoolean( PwmSetting.HELPDESK_ENABLE ) )
        {
            final String helpdeskProfileID = userInfo.getProfileIDs().get( ProfileDefinition.Helpdesk );
            if ( StringUtil.notEmpty( helpdeskProfileID ) )
            {
                final HelpdeskProfile helpdeskProfile = domainConfig.getHelpdeskProfiles().get( helpdeskProfileID );
                final long helpdeskIdleTimeout = helpdeskProfile.readSettingAsLong( PwmSetting.HELPDESK_IDLE_TIMEOUT_SECONDS );
                results.add( new MaxIdleTimeoutResult(
                        MaxIdleTimeoutResult.reasonFor( PwmSetting.HELPDESK_IDLE_TIMEOUT_SECONDS, helpdeskProfileID ),
                        TimeDuration.of( helpdeskIdleTimeout, TimeDuration.Unit.SECONDS ) ) );
            }
        }

        if ( domainConfig.readSettingAsBoolean( PwmSetting.PEOPLE_SEARCH_ENABLE ) )
        {
            final String peopleSearchID = userInfo.getProfileIDs().get( ProfileDefinition.PeopleSearch );
            if ( StringUtil.notEmpty( peopleSearchID ) )
            {
                final PeopleSearchProfile peopleSearchProfile = domainConfig.getPeopleSearchProfiles().get( peopleSearchID );
                final long peopleSearchIdleTimeout = peopleSearchProfile.readSettingAsLong( PwmSetting.PEOPLE_SEARCH_IDLE_TIMEOUT_SECONDS );
                if ( peopleSearchIdleTimeout > 0 )
                {
                    results.add( new MaxIdleTimeoutResult(
                            MaxIdleTimeoutResult.reasonFor( PwmSetting.PEOPLE_SEARCH_IDLE_TIMEOUT_SECONDS, peopleSearchID ),
                            TimeDuration.of( peopleSearchIdleTimeout, TimeDuration.Unit.SECONDS ) ) );
                }

            }
        }

        if ( userIsAdmin )
        {
            final long configEditorIdleTimeout = Long.parseLong( domainConfig.readAppProperty( AppProperty.CONFIG_EDITOR_IDLE_TIMEOUT ) );
            results.add( new MaxIdleTimeoutResult(
                    "Config Editor Idle Timeout",
                    TimeDuration.of( configEditorIdleTimeout, TimeDuration.Unit.SECONDS ) ) );
        }

        return Collections.unmodifiableSet( results );
    }

    @Value
    static class MaxIdleTimeoutResult implements Comparable<MaxIdleTimeoutResult>
    {
        private final String reason;
        private final TimeDuration idleTimeout;

        @Override
        public int compareTo( final MaxIdleTimeoutResult o )
        {
            return this.idleTimeout.compareTo( o.getIdleTimeout() );
        }

        static String reasonFor( final PwmSetting pwmSetting, final String profileID )
        {
            return "Setting " + pwmSetting.toMenuLocationDebug( profileID, PwmConstants.DEFAULT_LOCALE );
        }
    }

    public static TimeDuration idleTimeoutForRequest( final PwmRequest pwmRequest )
            throws PwmUnrecoverableException
    {
        final PwmURL pwmURL = pwmRequest.getURL();
        final PwmDomain pwmDomain = pwmRequest.getPwmDomain();
        final PwmSession pwmSession = pwmRequest.getPwmSession();

        if ( pwmURL.isResourceURL() )
        {
            return figureMaxSessionTimeout( pwmDomain, pwmSession ).getIdleTimeout();
        }

        final DomainConfig config = pwmDomain.getConfig();
        if ( pwmURL.matches( PwmServletDefinition.Helpdesk ) )
        {
            if ( config.readSettingAsBoolean( PwmSetting.HELPDESK_ENABLE ) )
            {
                final HelpdeskProfile helpdeskProfile = pwmRequest.getHelpdeskProfile( );
                if ( helpdeskProfile != null )
                {
                    final long helpdeskIdleTimeout = helpdeskProfile.readSettingAsLong( PwmSetting.HELPDESK_IDLE_TIMEOUT_SECONDS );
                    if ( helpdeskIdleTimeout > 0 )
                    {
                        return TimeDuration.of( helpdeskIdleTimeout, TimeDuration.Unit.SECONDS );
                    }
                }
            }
        }

        if (
                (
                        pwmURL.matches( PwmServletDefinition.PrivatePeopleSearch )
                                || pwmURL.matches( PwmServletDefinition.PublicPeopleSearch )
                )
                        && pwmURL.isPrivateUrl()
                )
        {
            final PeopleSearchProfile peopleSearchProfile = pwmRequest.getPeopleSearchProfile( );
            if ( peopleSearchProfile != null )
            {
                final long peopleSearchIdleTimeout = peopleSearchProfile.readSettingAsLong( PwmSetting.PEOPLE_SEARCH_IDLE_TIMEOUT_SECONDS );
                if ( peopleSearchIdleTimeout > 0 )
                {
                    return TimeDuration.of( peopleSearchIdleTimeout, TimeDuration.Unit.SECONDS );
                }
            }
        }

        if ( pwmURL.matches( PwmServletDefinition.ConfigEditor ) )
        {
            try
            {
                if ( pwmSession.getSessionManager().checkPermission( pwmDomain, Permission.PWMADMIN ) )
                {
                    final long configEditorIdleTimeout = Long.parseLong( config.readAppProperty( AppProperty.CONFIG_EDITOR_IDLE_TIMEOUT ) );
                    if ( configEditorIdleTimeout > 0 )
                    {
                        return TimeDuration.of( configEditorIdleTimeout, TimeDuration.Unit.SECONDS );
                    }
                }
            }
            catch ( final PwmUnrecoverableException e )
            {
                LOGGER.error( pwmRequest, () -> "error while figuring max idle timeout for session: " + e.getMessage() );
            }
        }

        if ( pwmURL.matches( PwmServletDefinition.ConfigGuide ) )
        {
            if ( pwmDomain.getApplicationMode() == PwmApplicationMode.NEW )
            {
                final long configGuideIdleTimeout = Long.parseLong( config.readAppProperty( AppProperty.CONFIG_GUIDE_IDLE_TIMEOUT ) );
                if ( configGuideIdleTimeout > 0 )
                {
                    return TimeDuration.of( configGuideIdleTimeout, TimeDuration.Unit.SECONDS );
                }
            }
        }

        final long idleTimeout = config.readSettingAsLong( PwmSetting.IDLE_TIMEOUT_SECONDS );
        return TimeDuration.of( idleTimeout, TimeDuration.Unit.SECONDS );
    }
}
