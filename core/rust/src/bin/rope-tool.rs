use std::fs;
use std::path::PathBuf;

use rope_core::{parse_envelope_meta, DeviceIdentity};

fn main() {
    let mut args = std::env::args().skip(1).collect::<Vec<_>>();
    if args.is_empty() {
        eprintln!("usage: rope-tool generate-identities <out-dir>");
        eprintln!("       rope-tool encrypt <alice.ropi> <bob.ropp> <text> <out.bin>");
        std::process::exit(2);
    }
    match args.remove(0).as_str() {
        "generate-identities" => {
            let dir = PathBuf::from(args.first().map(String::as_str).unwrap_or("protocol/testdata"));
            fs::create_dir_all(&dir).unwrap();
            let alice = DeviceIdentity::generate();
            let bob = DeviceIdentity::generate();
            fs::write(dir.join("alice.ropi"), alice.to_bytes()).unwrap();
            fs::write(dir.join("bob.ropi"), bob.to_bytes()).unwrap();
            fs::write(dir.join("alice.ropp"), alice.public_identity().blob).unwrap();
            fs::write(dir.join("bob.ropp"), bob.public_identity().blob).unwrap();
            fs::write(dir.join("alice.id"), alice.device_id()).unwrap();
            fs::write(dir.join("bob.id"), bob.device_id()).unwrap();
            println!("wrote identities to {}", dir.display());
        }
        "encrypt" => {
            let alice = DeviceIdentity::from_bytes(fs::read(&args[0]).unwrap()).unwrap();
            let bob_blob = fs::read(&args[1]).unwrap();
            let bob = rope_core::public_identity_from_blob(bob_blob).unwrap();
            let env = alice.encrypt_message(bob, args[2].clone()).unwrap();
            fs::write(&args[3], &env.bytes).unwrap();
            let meta = parse_envelope_meta(&env.bytes).unwrap();
            println!("{}", meta.message_id);
        }
        other => {
            eprintln!("unknown command {other}");
            std::process::exit(2);
        }
    }
}
