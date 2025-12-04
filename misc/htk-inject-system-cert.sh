set -e # Fail on error
# Create a separate temp directory, to hold the current certificates
# Without this, when we add the mount we can't read the current certs anymore.
mkdir -m 700 /data/local/tmp/htk-ca-copy
# Copy out the existing certificates
cp /system/etc/security/cacerts/* /data/local/tmp/htk-ca-copy/
# Create the in-memory mount on top of the system certs folder
mount -t tmpfs tmpfs /system/etc/security/cacerts
# Copy the existing certs back into the tmpfs mount, so we keep trusting them
mv /data/local/tmp/htk-ca-copy/* /system/etc/security/cacerts/
# Copy our new cert in, so we trust that too
cp /data/local/tmp/c88f7ed0.0 /system/etc/security/cacerts/
# Update the perms & selinux context labels, so everything is as readable as before
chown root:root /system/etc/security/cacerts/*
chmod 644 /system/etc/security/cacerts/*
chcon u:object_r:system_file:s0 /system/etc/security/cacerts/*
# Delete the temp cert directory & this script itself
rm -r /data/local/tmp/htk-ca-copy
# rm ${injectionScriptPath}
echo "System cert successfully injected"
