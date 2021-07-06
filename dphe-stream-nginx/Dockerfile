# Parent image
FROM centos:7

# Change to directory that contains the Dockerfile
WORKDIR /usr/src/app

# Copy from host to image
COPY . .

# Nginx package from EPEL is old, we create a new repository file to install the latest mainline version of Nginx
RUN echo $'[nginx-mainline]\n\
name=nginx mainline repo\n\
baseurl=http://nginx.org/packages/mainline/centos/7/$basearch/\n\
gpgcheck=0\n\
enabled=1\n'\
>> /etc/yum.repos.d/nginx.repo

# Reduce the number of layers in image by minimizing the number of separate RUN commands
# Update packages
# Install nginx (using the custom yum repo specified earlier)
# Remove the default nginx config file
# Overwrite the nginx.conf with ours to run nginx as non-root
# Install Extra Packages for Enterprise Linux (EPEL) 
# Use the EPEL repo for installing python, pip
# Upgrade pip, after upgrading, both pip and pip3 are the same version
# Install the Python dependencies of integration test
# Clean all yum cache
RUN yum update -y && \
    yum install -y nginx && \
    rm /etc/nginx/conf.d/default.conf && \
    mv nginx.conf /etc/nginx/nginx.conf && \
    yum install -y epel-release && \
    yum install -y python36 python36-pip && \
    pip3 install --upgrade pip && \
    pip install -r integration-test/requirements.txt && \
    yum clean all

# The EXPOSE instruction informs Docker that the container listens on the specified network ports at runtime. 
# EXPOSE does not make the ports of the container accessible to the host.
# Here 8080 for nginx running as non-root
EXPOSE 8080

# Install gosu for de-elevating root to deepphe user
RUN curl -o /usr/local/bin/gosu -SL "https://github.com/tianon/gosu/releases/download/1.12/gosu-amd64" && \
    curl -o /usr/local/bin/gosu.asc -SL "https://github.com/tianon/gosu/releases/download/1.12/gosu-amd64.asc" && \
    rm /usr/local/bin/gosu.asc && \
    chmod +x /usr/local/bin/gosu
    
# Set an entrypoint
RUN cp /usr/src/app/entrypoint.sh /usr/local/bin/entrypoint.sh && \
    chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

CMD ["nginx", "-g", "daemon off;"]