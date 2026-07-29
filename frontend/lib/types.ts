export type PartyRole = "OWNER" | "NARRATOR" | "PLAYER";
export type ChronicleType = "WRITTEN" | "GAME";
export type ChronicleStatus = "DRAFT" | "IN_PROGRESS" | "AI_PENDING" | "AI_PROCESSING" | "PUBLISHED" | "FAILED" | "ARCHIVED";

export interface AuthResponse {
  token: string;
  userId: string;
  username: string;
  displayName: string;
}

export interface PartySummary {
  id: string;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  role: PartyRole;
}

export interface PartyMember {
  userId: string;
  username: string;
  displayName: string;
  role: PartyRole;
  status: "ACTIVE" | "DISABLED" | "REMOVED";
}

export interface PartyDetail extends PartySummary {
  ownerId: string;
  currentUserRole: PartyRole;
  members: PartyMember[];
}

export interface PartyInvitationLink {
  partyId: string;
  inviteUrl: string;
}

export interface ChronicleCard {
  id: string;
  type: ChronicleType;
  status: ChronicleStatus;
  title: string;
  preview?: string;
  creatorName: string;
  updatedAt: string;
  published: boolean;
}

export interface Turn {
  id: string;
  sequenceNumber: number;
  cycleNumber: number;
  positionInCycle: number;
  userId: string;
  author: string;
  status: "WAITING" | "ACTIVE" | "SUBMITTED" | "SKIPPED" | "EXPIRED";
  startedAt?: string;
  expiresAt?: string;
}

export interface Segment {
  id: string;
  sequenceNumber: number;
  cycleNumber: number;
  authorId: string;
  author: string;
  status: "ACTIVE" | "EDITED" | "DISABLED";
  disabledReason?: string;
  visible: boolean;
  content?: string;
  size: "SHORT" | "MEDIUM" | "LONG" | "EXTRA_LONG";
  submittedAt: string;
}

export interface GeneratedStory {
  id: string;
  version: number;
  title: string;
  content: string;
  model: string;
  createdAt: string;
}

export interface GameDetail {
  id: string;
  title: string;
  status: ChronicleStatus;
  cycleCount: number;
  currentSequence: number;
  totalTurns: number;
  currentUserId: string;
  currentUserTurn: boolean;
  narrator: boolean;
  revealSeconds: number;
  currentDraft?: string;
  generatedStory?: GeneratedStory;
  turns: Turn[];
  segments: Segment[];
}

export interface WrittenDetail {
  id: string;
  title: string;
  status: ChronicleStatus;
  content: string;
  contentVersion: number;
  canEdit: boolean;
  lockedBy?: string;
  lockExpiresAt?: string;
  editorIds: string[];
}
