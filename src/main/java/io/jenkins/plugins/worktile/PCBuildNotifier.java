package io.jenkins.plugins.worktile;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import io.jenkins.plugins.worktile.model.WTBuildEntity;
import io.jenkins.plugins.worktile.service.WTRestService;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;

public class PCBuildNotifier extends Notifier implements SimpleBuildStep {

    private String overview;

    private String defaultSummary;

    private String resultURL;

    @DataBoundConstructor
    public PCBuildNotifier(String overview, String defaultSummary) {
        setOverview(overview);
        setDefaultSummary(defaultSummary);
    }

    public String getResultURL() {
        return resultURL;
    }

    @DataBoundSetter
    public void setResultURL(String resultURL) {
        this.resultURL = resultURL;
    }

    public String getDefaultSummary() {
        return defaultSummary;
    }

    @DataBoundSetter
    public void setDefaultSummary(String defaultSummary) {
        this.defaultSummary = Util.fixEmptyAndTrim(defaultSummary);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
            @Nonnull TaskListener listener) throws IOException, InternalError {
        this.createBuild(run, workspace, listener);
    }

    private void createBuild(Run<?, ?> run, FilePath workspace, @Nonnull TaskListener listener) throws IOException {
        WTLogger logger = new WTLogger(listener);
        WTBuildEntity entity = WTBuildEntity.from(run, workspace, listener, getOverview(), getDefaultSummary(),
                getResultURL());

        WTRestService service = new WTRestService();
        logger.info("Will send data to pingcode: " + entity.toString());
        try {
            service.createBuild(entity);
            logger.info("Create pingcode build record successfully.");
        } //
        catch (Exception error) {
            logger.error(error.getMessage());
        }
    }

    public String getOverview() {
        return overview;
    }

    @DataBoundSetter
    public void setOverview(String overview) {
        this.overview = Util.fixEmptyAndTrim(overview);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Publisher> {
        public Descriptor() {
            super(PCBuildNotifier.class);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return Messages.PCBuildNotifier_DisplayName();
        }

        @Override
        public PCBuildNotifier newInstance(StaplerRequest request, @NotNull JSONObject formData) throws FormException {
            assert request != null;
            return request.bindJSON(PCBuildNotifier.class, formData);
        }
    }
}
