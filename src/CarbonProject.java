public class CarbonProject {

    private int projectId;
    private String projectName;
    private String projectType;
    private String location;
    private int availableCredits;
    private double pricePerCredit;
    private String verificationStandard;
    private int vintageYear;

    public CarbonProject(
            int projectId,
            String projectName,
            String projectType,
            String location,
            int availableCredits,
            double pricePerCredit,
            String verificationStandard,
            int vintageYear) {

        this.projectId = projectId;
        this.projectName = projectName;
        this.projectType = projectType;
        this.location = location;
        this.availableCredits = availableCredits;
        this.pricePerCredit = pricePerCredit;
        this.verificationStandard = verificationStandard;
        this.vintageYear = vintageYear;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectType() {
        return projectType;
    }

    public String getLocation() {
        return location;
    }

    public int getAvailableCredits() {
        return availableCredits;
    }

    public double getPricePerCredit() {
        return pricePerCredit;
    }

    public String getVerificationStandard() {
        return verificationStandard;
    }

    public int getVintageYear() {
        return vintageYear;
    }
}