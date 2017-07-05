package org.wso2.sample.workflow.executor;

import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.WorkflowResponse;
import org.wso2.carbon.apimgt.impl.dto.WorkflowDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.workflow.GeneralWorkflowResponse;
import org.wso2.carbon.apimgt.impl.workflow.UserSignUpWSWorkflowExecutor;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowConstants;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.stream.XMLStreamException;

public class CustomUserSignUpWSWorkflowExecutor extends UserSignUpWSWorkflowExecutor {
    
    private static final Log log = LogFactory.getLog(CustomUserSignUpWSWorkflowExecutor.class);

    public static final String REGISTER_USER_PAYLOAD_CUSTOM =
            "	  <wor:UserSignupProcessRequest xmlns:wor=\"http://workflow.registeruser.apimgt.carbon.wso2.org\">\n" +
                    "         <wor:userName>$1</wor:userName>\n" +
                    "         <wor:tenantDomain>$2</wor:tenantDomain>\n" +
                    "         <wor:workflowExternalRef>$3</wor:workflowExternalRef>\n" +
                    "         <wor:callBackURL>$4</wor:callBackURL>\n" +
                    "         <wor:organization>$5</wor:organization>\n" +
                    "         <wor:country>$6</wor:country>\n" +
                    "      </wor:UserSignupProcessRequest>";

    @Override
    public WorkflowResponse execute(WorkflowDTO workflowDTO) throws WorkflowException {

        if (log.isDebugEnabled()) {
            log.debug("Executing User SignUp Webservice Workflow for " + workflowDTO.getWorkflowReference());
        }

        try {
            String action = WorkflowConstants.REGISTER_USER_WS_ACTION;
            ServiceClient client = getClient(action);

            //defining the empty payload
            String payload = REGISTER_USER_PAYLOAD_CUSTOM;

            String callBackURL = workflowDTO.getCallbackUrl();
            String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(workflowDTO.getWorkflowReference());

            //getting the user store manager
            RealmService realmService = ServiceReferenceHolder.getInstance().getRealmService();
            UserRealm realm = realmService.getBootstrapRealm();
            UserStoreManager manager = realm.getUserStoreManager();

            //getting the user claim value
            String organization = manager.getUserClaimValue(tenantAwareUserName, "http://wso2.org/claims/organization", null);
            String country = manager.getUserClaimValue(tenantAwareUserName, "http://wso2.org/claims/country", null);

            //assigning values to the payload
            payload = payload.replace("$1", tenantAwareUserName);
            payload = payload.replace("$2", workflowDTO.getTenantDomain());
            payload = payload.replace("$3", workflowDTO.getExternalWorkflowReference());
            payload = payload.replace("$4", callBackURL != null ? callBackURL : "?");
            payload = payload.replace("$5", organization);
            payload = payload.replace("$6", country);

            client.fireAndForget(AXIOMUtil.stringToOM(payload));
            super.execute(workflowDTO);
        } catch (AxisFault axisFault) {
            log.error("Error sending out message", axisFault);
            throw new WorkflowException("Error sending out message", axisFault);
        } catch (XMLStreamException e) {
            log.error("Error converting String to OMElement", e);
            throw new WorkflowException("Error converting String to OMElement", e);
        } catch (UserStoreException e) {
            e.printStackTrace();
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new GeneralWorkflowResponse();
    }
}
