#!/usr/bin/env bash -xeu

# Script to install the latest CLI with a bash wrapper
#
# If the script is hosted, the install can be done with:
#
# bash 4:
# source <(curl -L -N -s {{scheme}}://{{host}}:{{port}}/cli/install.sh)
#
# bash 3 (mac):
# source /dev/stdin <<< "$(curl -L -N -s {{scheme}}://{{host}}:{{port}}/cli/install.sh)"
#
# if not sourcing:
# bash <(curl -L -N -s {{scheme}}://{{host}}:{{port}}/cli/install.sh)
#
# Optional: specify the install directory: ?installDirectory=mydirectory

# Prep the install dir
mkdir -p {{installDirectory}}

# Create the bash wrapper for the CLI
cat > {{installDirectory}}/mojito << EOF
#!/usr/bin/env bash
java -Dl10n.resttemplate.host={{host}} \\
     -Dl10n.resttemplate.scheme={{scheme}} \\
     -Dl10n.resttemplate.port={{port}} \\
     -Dlogging.path={{installDirectory}} \\
     -jar {{installDirectory}}/mojito-cli.jar "\$@" ;
EOF

# Make the wrapper executable
chmod +x {{installDirectory}}/mojito

# Export the PATH to have access to the bash wrapper once installation is done
export PATH={{installDirectory}}:${PATH}

# Download/Upgrade the jar file if needed to match server version
#
# Upgrade disabled for now, server not deployed:
mojito --check-server-version 2>/dev/null || curl -L -s -o {{installDirectory}}/mojito-cli.jar {{scheme}}://{{host}}:{{port}}/cli/mojito-cli.jar
