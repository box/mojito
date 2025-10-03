#!/usr/bin/env bash -xeu

# Script to install the latest CLI with a bash wrapper
#
# If the script is hosted, the install can be done with:
#
# bash 4:
# source <(curl -L -N -s http://localhost:8080/cli/install.sh)
#
# bash 3 (mac):
# source /dev/stdin <<< "$(curl -L -N -s http://localhost:8080/cli/install.sh)"
#
# if not sourcing:
# bash <(curl -L -N -s http://localhost:8080/cli/install.sh)
#
# Optional: specify the install directory: ?installDirectory=mydirectory

# Prep the install dir
mkdir -p ${PWD}/.mojito

# Create the bash wrapper for the CLI
cat > ${PWD}/.mojito/mojito-tmp << 'EOF'
#!/usr/bin/env bash
java -Dl10n.resttemplate.host=localhost \
     -Dl10n.resttemplate.scheme=http \
     -Dl10n.resttemplate.port=8080 \
     -Dlogging.file.path=${PWD}/.mojito \
     -jar ${PWD}/.mojito/mojito-cli.jar "$@" ;
EOF

_ESC="$(printf '%s' "${PWD}" | sed 's/[\/&]/\\&/g')"
sed "s|\${PWD}|$_ESC|g" ${PWD}/.mojito/mojito-tmp > ${PWD}/.mojito/mojito
rm ${PWD}/.mojito/mojito-tmp

# Make the wrapper executable
chmod +x ${PWD}/.mojito/mojito

# Export the PATH to have access to the bash wrapper once installation is done
export PATH=${PWD}/.mojito:${PATH}

# Download/Upgrade the jar file if needed to match server version
mojito --check-server-version 2>/dev/null || curl -L -s -o ${PWD}/.mojito/mojito-cli.jar http://localhost:8080/cli/mojito-cli.jar
