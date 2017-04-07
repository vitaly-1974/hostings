package hosting;

public class host {
String              name, 
                    ping, 
                     ssh, 
                    swap, 
                 mem_tot, 
       disk_ag_root_free, 
                cpu_name, 
            cpu_features, 
                location, 
                   state, 
                     oss, 
                    rams, 
                 comment, 
               requested, 
                   group, 
               cygw_vers, 
                 job_url,
                 failure,
                    uses;
int          failed_jobs,
                   cores;

host(String name) {
    this.name = name;
}

void set_name(String name) {
    this.name = name;
}

host(String name, String uses, String requested) {
    this.name = name;
    this.uses = uses;
    this.requested = requested;
}

host(String name, int failed_jobs, String failure, String job_url) {
    this.name = name;
    this.failed_jobs = failed_jobs;
    this.failure = failure;
    this.job_url = job_url;
}


void set_attribute_disabled(int failed_jobs, String failure, String job_url) {
    this.failed_jobs = failed_jobs;
    this.failure = failure;
    this.job_url = job_url;
}


}
