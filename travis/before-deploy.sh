#!/usr/bin/env bash

#end_with_error()
#{
#echo "ERROR: ${1:-"Unknown Error"} Exiting." 1>&2
#exit 1
#}

#declare -r secring_auto="${GPG_DIR}/secring.auto"
#declare -r pubring_auto="${GPG_DIR}/pubring.auto"

#echo
#echo "Import signer public key..."
#gpg --import "${GPG_DIR}/signer.pub.asc"

#echo
#echo "Decrypting secret gpg keyring.."
#{ echo $GPG_PASSWORD | gpg --passphrase-fd 0 "${secring_auto}".gpg ; } || { end_with_error "Failed to decrypt secret gpg keyring." ; }
#echo "Success!"

#echo
#echo "Importing keyrings.."
#{ gpg --home "${GPG_DIR}" --import "${secring_auto}" ; } || { end_with_error "Could not import secret keyring into gpg." ; }
#{ gpg --home "${GPG_DIR}" --import "${pubring_auto}" ; } || { end_with_error "Could not import public keyring into gpg." ; }
#echo Success!
