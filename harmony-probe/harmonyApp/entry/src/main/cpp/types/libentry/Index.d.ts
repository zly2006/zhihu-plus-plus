import { ArkUIViewController } from "compose/src/main/cpp/types/libcompose_arkui_utils";

export const MainArkUIViewController: () => ArkUIViewController
export const applyHomeJson: (value: string) => void
export const applyDetailJson: (value: string) => void
export const applyImageBase64: (value: string) => void
export const applySessionStatus: (value: string) => void
export const applyError: (value: string) => void
export const applyColorMode: (value: string) => void
export const usesNativeNetwork: () => boolean
export const handleBack: () => boolean
