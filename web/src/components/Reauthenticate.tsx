"use client";
import { useEffect, useState } from "react";
import { EmailAuthProvider, reauthenticateWithCredential } from "firebase/auth";
import { identity, setReauthenticationHandler } from "@/lib/api";
import { Drawer } from "./ui";
import { Form } from "./Forms";

export default function Reauthenticate() {
  const [pending, setPending] = useState<{
    resolve: () => void;
    reject: (e: Error) => void;
  } | null>(null);
  useEffect(
    () =>
      setReauthenticationHandler(
        () =>
          new Promise<void>((resolve, reject) =>
            setPending({ resolve, reject }),
          ),
      ),
    [],
  );
  if (!pending) return null;
  return (
    <Drawer
      title="Confirm it's you"
      onClose={() => {
        pending.reject(
          Error("Confirmation cancelled. Your changes were not submitted."),
        );
        setPending(null);
      }}
    >
      <p>
        Confirm your password to make this sensitive change. Your current work
        stays open.
      </p>
      <Form
        fields={[
          { key: "password", label: "Password", type: "password", full: true },
        ]}
        submit="Confirm password"
        onSubmit={async (v) => {
          const user = (await identity()).currentUser;
          if (!user?.email) throw Error("Sign in to continue.");
          await reauthenticateWithCredential(
            user,
            EmailAuthProvider.credential(user.email, v.password),
          );
          await user.getIdToken(true);
          pending.resolve();
          setPending(null);
        }}
      />
    </Drawer>
  );
}
