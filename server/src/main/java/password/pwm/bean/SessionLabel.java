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

package password.pwm.bean;

import lombok.Builder;
import lombok.Value;
import password.pwm.PwmConstants;
import password.pwm.svc.PwmService;

import java.io.Serializable;

@Value
@Builder( toBuilder = true )
public class SessionLabel implements Serializable
{
    public static final String SESSION_LABEL_SESSION_ID = "#";
    public static final SessionLabel SYSTEM_LABEL = SessionLabel.builder().sessionID( SESSION_LABEL_SESSION_ID ).username( PwmConstants.PWM_APP_NAME ).build();
    public static final SessionLabel TEST_SESSION_LABEL = SessionLabel.builder().sessionID( SESSION_LABEL_SESSION_ID ).username( "test" ).build();
    public static final SessionLabel CLI_SESSION_LABEL = SessionLabel.builder().sessionID( SESSION_LABEL_SESSION_ID ).username( "cli" ).build();
    public static final SessionLabel CONTEXT_SESSION_LABEL = SessionLabel.builder().sessionID( SESSION_LABEL_SESSION_ID ).username( "context" ).build();

    private final String sessionID;
    private final String requestID;
    private final String userID;
    private final String username;
    private final String sourceAddress;
    private final String sourceHostname;
    private final String profile;
    private final String domain;

    public static SessionLabel forPwmService( final PwmService pwmService, final DomainID domainID )
    {
        return SessionLabel.builder()
                .sessionID( SESSION_LABEL_SESSION_ID )
                .username( pwmService.getClass().getSimpleName() )
                .domain( domainID.stringValue() )
                .build();
    }
}
