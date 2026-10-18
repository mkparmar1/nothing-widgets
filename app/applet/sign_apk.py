import os
import sys
import struct
import hashlib
import base64
import subprocess
import zipfile

def modify_manifest(mf_data: bytes) -> bytes:
    mf = bytearray(mf_data)
    # Parse string pool
    sp_type, sp_hsize, sp_size, str_count, sty_count, flags, str_start, sty_start = struct.unpack('<2H6I', mf[8:36])
    offsets = struct.unpack(f'<{str_count}I', mf[36:36 + 4 * str_count])
    str_bytes = mf[8 + str_start:8 + (sty_start if sty_count else sp_size)]
    
    strings = []
    for off in offsets:
        p = off
        u16len = struct.unpack('<H', str_bytes[p:p+2])[0]
        s = str_bytes[p+2:p+2+u16len*2].decode('utf-16le', errors='replace')
        strings.append(s)

    # Replace preview codename '16' with release standard 'REL'
    if len(strings) > 30 and strings[30] == '16':
        print(f"Replacing string 30 '{strings[30]}' with 'REL'")
        strings[30] = 'REL'

    # Rebuild UTF-16 string pool
    new_offsets = []
    new_str_data = bytearray()
    for s in strings:
        new_offsets.append(len(new_str_data))
        encoded = s.encode('utf-16le')
        new_str_data.extend(struct.pack('<H', len(s)) + encoded + b'\x00\x00')

    while len(new_str_data) % 4 != 0:
        new_str_data.append(0)

    new_str_start = 28 + 4 * str_count
    new_sp_size = new_str_start + len(new_str_data)
    new_sp = struct.pack('<2H6I', 0x0001, 28, new_sp_size, str_count, 0, flags, new_str_start, 0)
    new_sp += struct.pack(f'<{str_count}I', *new_offsets)
    new_sp += new_str_data

    # Rest of the XML
    rest_xml = bytearray(mf[8 + sp_size:])

    # Walk XML tags in rest_xml and update SDK integers
    pos = 0
    ctype, csize = struct.unpack('<II', rest_xml[pos:pos+8])
    if ctype == 0x0180: # RES_XML_RESOURCE_MAP_TYPE
        pos += csize

    while pos < len(rest_xml):
        ctype, hsize, csize = struct.unpack('<HHI', rest_xml[pos:pos+8])
        if ctype == 0x0102: # START_TAG
            ln, comment, ns, name = struct.unpack('<4I', rest_xml[pos+8:pos+24])
            a_start, a_size, a_count = struct.unpack('<HHI', rest_xml[pos+24:pos+32])[:3]
            tag = strings[name] if name < len(strings) else ''
            
            for i in range(a_count):
                ap = pos + 16 + a_start + i * a_size
                ans, aname, aval_str, atype, adata = struct.unpack('<5I', rest_xml[ap:ap+20])
                ak = strings[aname] if aname < len(strings) else ''
                
                # We want stable Android 15 (API 35) target and compile
                if tag == 'manifest' and ak == 'compileSdkVersion':
                    print(f"Setting compileSdkVersion from {adata} to 35")
                    struct.pack_into('<I', rest_xml, ap + 16, 35)
                elif tag == 'manifest' and ak == 'platformBuildVersionCode':
                    print(f"Setting platformBuildVersionCode from {adata} to 35")
                    struct.pack_into('<I', rest_xml, ap + 16, 35)
                elif tag == 'uses-sdk' and ak == 'targetSdkVersion':
                    print(f"Setting targetSdkVersion from {adata} to 35")
                    struct.pack_into('<I', rest_xml, ap + 16, 35)
                elif tag == 'receiver' and ak == 'exported':
                    rec_name = ''
                    for j in range(a_count):
                        jap = pos + 16 + a_start + j * a_size
                        _, janame, javal_str, _, jadata = struct.unpack('<5I', rest_xml[jap:jap+20])
                        if strings[janame] == 'name' and javal_str < len(strings):
                            rec_name = strings[javal_str]
                    if 'WidgetConfigReceiver' in rec_name:
                        print("Setting WidgetConfigReceiver exported to false (0)")
                        struct.pack_into('<I', rest_xml, ap + 16, 0)

        pos += csize

    new_total_size = 8 + new_sp_size + len(rest_xml)
    new_xml_header = struct.pack('<2HI', 0x0003, 8, new_total_size)
    return bytes(new_xml_header + new_sp + rest_xml)

def run():
    in_apk = './public/app-debug.apk'
    out_apk = '/tmp/rebuilt-debug.apk'

    # Extract private key and cert
    subprocess.run("openssl pkcs12 -in debug.keystore -nodes -passin pass:android -nocerts -out /tmp/key.pem", shell=True, check=True)
    subprocess.run("openssl pkcs12 -in debug.keystore -nodes -passin pass:android -nokeys -out /tmp/cert.pem", shell=True, check=True)
    subprocess.run("openssl x509 -in /tmp/cert.pem -outform DER -out /tmp/cert.der", shell=True, check=True)
    subprocess.run("openssl x509 -in /tmp/cert.pem -pubkey -noout -out /tmp/pubkey.pem", shell=True, check=True)
    subprocess.run("openssl rsa -pubin -in /tmp/pubkey.pem -outform DER -out /tmp/pubkey.der", shell=True, check=True)

    with open('/tmp/cert.der', 'rb') as f:
        cert_der = f.read()
    with open('/tmp/pubkey.der', 'rb') as f:
        pubkey_der = f.read()

    # Read original APK entries
    entries = []
    with zipfile.ZipFile(in_apk, 'r') as z:
        for info in z.infolist():
            # Skip old signature files
            if info.filename.startswith('META-INF/') and (
                info.filename.endswith('.SF') or 
                info.filename.endswith('.RSA') or 
                info.filename.endswith('.DSA') or 
                info.filename.endswith('.EC') or 
                info.filename == 'META-INF/MANIFEST.MF'
            ):
                continue
            data = z.read(info.filename)
            if info.filename == 'AndroidManifest.xml':
                data = modify_manifest(data)
            entries.append((info.filename, data, info.compress_type))

    # Generate V1 MANIFEST.MF and CERT.SF
    manifest_lines = [
        "Manifest-Version: 1.0",
        "Created-By: 1.0 (Android)",
        ""
    ]
    sf_lines = [
        "Signature-Version: 1.0",
        "Created-By: 1.0 (Android)",
    ]

    manifest_entries = {}
    for fn, data, _ in entries:
        if fn.startswith('META-INF/'):
            continue
        h = hashlib.sha256(data).digest()
        b64 = base64.b64encode(h).decode('ascii')
        entry_str = f"Name: {fn}\r\nSHA-256-Digest: {b64}\r\n\r\n"
        manifest_entries[fn] = entry_str

    full_manifest = "Manifest-Version: 1.0\r\nCreated-By: 1.0 (Android)\r\n\r\n" + "".join(manifest_entries.values())
    manifest_bytes = full_manifest.encode('utf-8')

    manifest_digest = base64.b64encode(hashlib.sha256(manifest_bytes).digest()).decode('ascii')
    sf_header = f"Signature-Version: 1.0\r\nCreated-By: 1.0 (Android)\r\nSHA-256-Digest-Manifest: {manifest_digest}\r\n\r\n"
    sf_body = []
    for fn, entry_str in manifest_entries.items():
        entry_h = base64.b64encode(hashlib.sha256(entry_str.encode('utf-8')).digest()).decode('ascii')
        sf_body.append(f"Name: {fn}\r\nSHA-256-Digest: {entry_h}\r\n\r\n")
    sf_bytes = (sf_header + "".join(sf_body)).encode('utf-8')

    with open('/tmp/CERT.SF', 'wb') as f:
        f.write(sf_bytes)

    # Sign CERT.SF to create detached CERT.RSA
    subprocess.run("openssl smime -sign -in /tmp/CERT.SF -out /tmp/CERT.RSA -outform DER -inkey /tmp/key.pem -signer /tmp/cert.pem -nosmimecap", shell=True, check=True)
    with open('/tmp/CERT.RSA', 'rb') as f:
        cert_rsa_bytes = f.read()

    # Prepend META-INF entries
    v1_entries = [
        ('META-INF/MANIFEST.MF', manifest_bytes, zipfile.ZIP_DEFLATED),
        ('META-INF/CERT.SF', sf_bytes, zipfile.ZIP_DEFLATED),
        ('META-INF/CERT.RSA', cert_rsa_bytes, zipfile.ZIP_STORED),
    ]

    all_entries = v1_entries + entries

    # Build ZIP with 16KB alignment for uncompressed .so files
    import io
    zip_buf = io.BytesIO()
    cd_entries = []

    for fn, data, comp_type in all_entries:
        is_so = fn.endswith('.so')
        if is_so:
            comp_type = zipfile.ZIP_STORED
            compressed_data = data
        elif comp_type == zipfile.ZIP_STORED:
            compressed_data = data
        else:
            comp_type = zipfile.ZIP_DEFLATED
            import zlib
            compressed_data = zlib.compress(data)[2:-4] # raw deflate

        crc = zlib.crc32(data) & 0xFFFFFFFF
        fn_bytes = fn.encode('utf-8')

        header_offset = zip_buf.tell()
        extra = b''
        if is_so and comp_type == zipfile.ZIP_STORED:
            # Align data_offset to 16384 bytes
            # data_offset = header_offset + 30 + len(fn_bytes) + len(extra)
            # We can use extra field for padding
            cur_offset = header_offset + 30 + len(fn_bytes)
            pad_needed = (16384 - (cur_offset % 16384)) % 16384
            if pad_needed > 0:
                if pad_needed < 4:
                    pad_needed += 16384
                # Extra field header: 2 bytes id (0xd935 android alignment), 2 bytes size
                extra = struct.pack('<HH', 0xd935, pad_needed - 4) + b'\x00' * (pad_needed - 4)

        # Local file header
        lfh = struct.pack(
            '<4s5H3I2H',
            b'PK\x03\x04',
            20, # version needed
            0,  # flags
            comp_type,
            0, 0, # mod time, mod date
            crc,
            len(compressed_data),
            len(data),
            len(fn_bytes),
            len(extra)
        )
        zip_buf.write(lfh)
        zip_buf.write(fn_bytes)
        zip_buf.write(extra)
        zip_buf.write(compressed_data)

        # Record for Central Directory
        cd_record = (fn_bytes, comp_type, crc, len(compressed_data), len(data), header_offset, extra)
        cd_entries.append(cd_record)

    cd_start = zip_buf.tell()
    for fn_bytes, comp_type, crc, comp_len, orig_len, h_offset, extra in cd_entries:
        cdh = struct.pack(
            '<4s6H3I5H2I',
            b'PK\x01\x02',
            20, # version made by
            20, # version needed
            0,  # flags
            comp_type,
            0, 0, # mod time, date
            crc,
            comp_len,
            orig_len,
            len(fn_bytes),
            len(extra),
            0, # comment len
            0, # disk start
            0, # internal attr
            0, # external attr
            h_offset
        )
        zip_buf.write(cdh)
        zip_buf.write(fn_bytes)
        zip_buf.write(extra)

    cd_size = zip_buf.tell() - cd_start

    # EOCD
    eocd = struct.pack(
        '<4s4H2IH',
        b'PK\x05\x06',
        0, 0,
        len(cd_entries),
        len(cd_entries),
        cd_size,
        cd_start,
        0
    )
    zip_buf.write(eocd)

    apk_bytes = zip_buf.getvalue()

    # Now generate APK Signature Scheme v2
    sig_block_start = cd_start
    part1 = apk_bytes[:sig_block_start]
    part2 = apk_bytes[cd_start:cd_start + cd_size]
    # In EOCD, cd_offset is replaced with sig_block_start
    eocd_pos = cd_start + cd_size
    part3 = apk_bytes[eocd_pos:eocd_pos+16] + struct.pack('<I', sig_block_start) + apk_bytes[eocd_pos+20:]

    # Compute chunked SHA-256
    chunk_size = 1024 * 1024
    chunk_hashes = []
    for part in [part1, part2, part3]:
        for i in range(0, len(part), chunk_size):
            chunk = part[i:i+chunk_size]
            h = hashlib.sha256(b'\xa5' + struct.pack('<I', len(chunk)) + chunk).digest()
            chunk_hashes.append(h)

    top_hash = hashlib.sha256(b'\x5a' + struct.pack('<I', len(chunk_hashes)) + b''.join(chunk_hashes)).digest()
    print("Computed top hash for v2:", top_hash.hex())

    # Build signed_data
    # digests: length-prefixed sequence of digests
    # each digest: len, algo (0x00000103 = RSA-SHA256), digest_len (32), digest
    digest_item = struct.pack('<III', 4 + 4 + 32, 0x00000103, 32) + top_hash
    digests_block = struct.pack('<I', len(digest_item)) + digest_item

    # certs: length-prefixed sequence of certs
    cert_item = struct.pack('<I', len(cert_der)) + cert_der
    certs_block = struct.pack('<I', len(cert_item)) + cert_item

    # attrs: empty
    attrs_block = struct.pack('<I', 0)

    signed_data = digests_block + certs_block + attrs_block
    signed_data_block = struct.pack('<I', len(signed_data)) + signed_data

    # Sign signed_data with RSA-SHA256
    with open('/tmp/signed_data.bin', 'wb') as f:
        f.write(signed_data)
    subprocess.run("openssl dgst -sha256 -sign /tmp/key.pem -out /tmp/v2_sig.bin /tmp/signed_data.bin", shell=True, check=True)
    with open('/tmp/v2_sig.bin', 'rb') as f:
        sig_bytes = f.read()

    # signatures: length-prefixed sequence
    sig_item = struct.pack('<III', 4 + 4 + len(sig_bytes), 0x00000103, len(sig_bytes)) + sig_bytes
    signatures_block = struct.pack('<I', len(sig_item)) + sig_item

    # public key
    pk_block = struct.pack('<I', len(pubkey_der)) + pubkey_der

    # signer
    signer = signed_data_block + signatures_block + pk_block
    signer_block = struct.pack('<I', len(signer)) + signer

    # signers block
    v2_value = struct.pack('<I', len(signer_block)) + signer_block

    # ID-value pair
    pair = struct.pack('<QI', len(v2_value) + 4, 0x7109871a) + v2_value

    # Full APK Signing Block
    total_block_size = len(pair) + 24
    signing_block = (
        struct.pack('<Q', total_block_size) +
        pair +
        struct.pack('<Q', total_block_size) +
        b'APK Sig Block 42'
    )

    # Reassemble final APK
    # New CD offset will be sig_block_start + len(signing_block)
    new_cd_start = sig_block_start + len(signing_block)
    new_eocd = struct.pack(
        '<4s4H2IH',
        b'PK\x05\x06',
        0, 0,
        len(cd_entries),
        len(cd_entries),
        cd_size,
        new_cd_start,
        0
    )

    final_apk = part1 + signing_block + part2 + new_eocd

    with open(out_apk, 'wb') as f:
        f.write(final_apk)

    print(f"Successfully generated signed APK at {out_apk}, size: {len(final_apk)} bytes")

run()
