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

package password.pwm.http.tag.value;

import password.pwm.AppProperty;
import password.pwm.Permission;
import password.pwm.PwmApplicationMode;
import password.pwm.PwmConstants;
import password.pwm.PwmDomain;
import password.pwm.config.PwmSetting;
import password.pwm.error.PwmUnrecoverableException;
import password.pwm.http.IdleTimeoutCalculator;
import password.pwm.http.PwmRequest;
import password.pwm.http.servlet.ClientApiServlet;
import password.pwm.i18n.Admin;
import password.pwm.util.i18n.LocaleHelper;
import password.pwm.util.java.PwmTimeUtil;
import password.pwm.util.logging.PwmLogger;
import password.pwm.util.macro.MacroRequest;

import javax.servlet.jsp.JspPage;
import javax.servlet.jsp.PageContext;
import java.util.Locale;

public class PwmValueHandlers
{
    private static final PwmLogger LOGGER = PwmLogger.forClass( PwmValueHandlers.class );



    static class CspNonceHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext ) throws PwmUnrecoverableException
        {
            return pwmRequest.getCspNonce();
        }
    }

    static class HomeUrlHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext ) throws PwmUnrecoverableException
        {
            String outputURL = pwmRequest.getDomainConfig().readSettingAsString( PwmSetting.URL_HOME );
            if ( outputURL == null || outputURL.isEmpty() )
            {
                outputURL = pwmRequest.getHttpServletRequest().getContextPath();
            }
            else
            {
                try
                {
                    final MacroRequest macroRequest = pwmRequest.getMacroMachine();
                    outputURL = macroRequest.expandMacros( outputURL );
                }
                catch ( final PwmUnrecoverableException e )
                {
                    LOGGER.error( pwmRequest, () -> "error expanding macros in homeURL: " + e.getMessage() );
                }
            }
            return outputURL;
        }
    }

    static class PasswordFieldTypeHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext ) throws PwmUnrecoverableException
        {
            final boolean maskPasswordFields = pwmRequest.getDomainConfig().readSettingAsBoolean( PwmSetting.DISPLAY_MASK_PASSWORD_FIELDS );
            return maskPasswordFields ? "password" : "text";
        }
    }

    static class ResponseFieldTypeHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext ) throws PwmUnrecoverableException
        {
            final boolean maskPasswordFields = pwmRequest.getDomainConfig().readSettingAsBoolean( PwmSetting.DISPLAY_MASK_RESPONSE_FIELDS );
            return maskPasswordFields ? "password" : "text";
        }
    }

    static class CustomJavascriptHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext ) throws PwmUnrecoverableException
        {
            final String customScript = pwmRequest.getDomainConfig().readSettingAsString(
                    PwmSetting.DISPLAY_CUSTOM_JAVASCRIPT );
            if ( customScript != null && !customScript.isEmpty() )
            {
                try
                {
                    final MacroRequest macroRequest = pwmRequest.getMacroMachine();
                    return macroRequest.expandMacros( customScript );
                }
                catch ( final Exception e )
                {
                    LOGGER.error( pwmRequest, () -> "error while expanding customJavascript macros: " + e.getMessage() );
                    return customScript;
                }
            }
            return "";
        }
    }

    static class CurrentJspFilenameHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext ) throws PwmUnrecoverableException
        {
            final JspPage jspPage = ( JspPage ) pageContext.getPage();
            if ( jspPage != null )
            {
                String name = jspPage.getClass().getSimpleName();
                name = name.replaceAll( "_002d", "-" );
                return name.replaceAll( "_", "." );
            }
            return "";
        }
    }

    static class InstanceIDHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext ) throws PwmUnrecoverableException
        {
            return pwmRequest.getPwmApplication().getInstanceID();

        }
    }

    static class HeaderMenuNoticeHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext ) throws PwmUnrecoverableException
        {
            final String[] fieldNames = new String[]
                    {
                            PwmConstants.PWM_APP_NAME,
                    };

            if ( PwmConstants.TRIAL_MODE )
            {
                return LocaleHelper.getLocalizedMessage( pwmRequest.getLocale(), "Header_TrialMode", pwmRequest.getDomainConfig(), Admin.class, fieldNames );
            }
            else if ( pwmRequest.getPwmDomain().getApplicationMode() == PwmApplicationMode.CONFIGURATION )
            {
                String output = "";
                if ( Boolean.parseBoolean( pwmRequest.getDomainConfig().readAppProperty( AppProperty.CLIENT_JSP_SHOW_ICONS ) ) )
                {
                    output += "<span id=\"icon-configModeHelp\" class=\"btn-icon pwm-icon pwm-icon-question-circle\"></span>";
                }
                return output + LocaleHelper.getLocalizedMessage( pwmRequest.getLocale(), "Header_ConfigModeActive", pwmRequest.getDomainConfig(), Admin.class, fieldNames );
            }
            else if ( pwmRequest.checkPermission( Permission.PWMADMIN ) )
            {
                return LocaleHelper.getLocalizedMessage( pwmRequest.getLocale(), "Header_AdminUser", pwmRequest.getDomainConfig(), Admin.class, fieldNames );
            }

            return "";
        }
    }

    static class ClientETag implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext )
                throws PwmUnrecoverableException
        {
            return ClientApiServlet.makeClientEtag( pwmRequest );
        }
    }

    static class LocaleCodeHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext )
        {
            return pwmRequest.getLocale().toLanguageTag();
        }
    }

    static class LocaleDirHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext )
        {
            final Locale locale = pwmRequest.getLocale();
            final PwmDomain pwmDomain = pwmRequest.getPwmDomain();
            final LocaleHelper.TextDirection textDirection = LocaleHelper.textDirectionForLocale( pwmDomain, locale );
            return textDirection.name();
        }
    }

    static class LocaleNameHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext )
        {
            final Locale locale = pwmRequest.getLocale();
            return locale.getDisplayName( locale );
        }
    }

    static class LocaleFlagFileHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext )
        {
            final String flagFileName = pwmRequest.getAppConfig().getKnownLocaleFlagMap().get( pwmRequest.getLocale() );
            return flagFileName == null ? "" : flagFileName;
        }
    }

    static class InactiveTimeRemainingHandlerPwm implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext )
                throws PwmUnrecoverableException
        {
            return PwmTimeUtil.asLongString( IdleTimeoutCalculator.idleTimeoutForRequest( pwmRequest ) );
        }
    }

    static class SessionIDPwmValue implements PwmValueHandler
    {
        @Override
        public String valueOutput( final PwmRequest pwmRequest, final PageContext pageContext )
                throws PwmUnrecoverableException
        {
            return pwmRequest.getPwmSession().getSessionStateBean().getSessionID();
        }
    }
}
