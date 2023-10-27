# vim:set ft=dockerfile:
ARG BASE_IMAGE

FROM ${BASE_IMAGE}

ARG VERSION
ARG SCM_REF
ARG BUILD_TAG
ARG CLID
ARG PACKAGES

LABEL com.customer.sample.version=${VERSION}
LABEL com.customer.sample.scm-ref=${SCM_REF}
LABEL com.customer.sample.build-tag=${BUILD_TAG}


# We need to be root to run yum commands
USER 0

RUN wget https://github.com/wkhtmltopdf/packaging/releases/download/0.12.6-1/wkhtmltox-0.12.6-1.centos7.x86_64.rpm && yum localinstall -y wkhtmltox-0.12.6-1.centos7.x86_64.rpm

ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US:en" \
    LC_ALL="en_US.UTF-8"

# Set back original (nuxeo) user
USER 900

COPY --chown=nuxeo:0 target/nuxeo-ldt-parser-package-*.zip /home/nuxeo/local-packages/nuxeo-ldt-parser-package.zip

RUN /install-packages.sh --offline /home/nuxeo/local-packages/nuxeo-ldt-parser-package.zip
